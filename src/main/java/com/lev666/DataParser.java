package com.lev666;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataParser {
    final org.slf4j.Logger logger = LoggerFactory.getLogger(DataParser.class);
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, String> voiceMess = new HashMap<>();
    private final List<Message> allMessages = new ArrayList<>();

    private static File dirMess;
    private final List<File> htmlFiles = new ArrayList<>();
    private File transcriptFile;
    private File customVoiceFile;
    private boolean checkModelSet = false;

    public static File getDirMess() {
        return dirMess;
    }

    public void startParsing() {
        promptForDirectoryPath();

        logger.info("Парсинг будет происходить для папки: {}", dirMess.getAbsolutePath());

        useAndDownlModel();
        scanFiles();
        parseTranscriptFile();
        parseHtmlFiles();
        parseToOutput();
        logger.info("Парсинг завершён.");
    }

    private void useAndDownlModel() {
        logger.info("Хотите использовать модель для распознавания речи? 1 - ДА, 2 - НЕТ");
        switch (scanner.nextLine().trim()) {
            case "1": {
                checkModelSet = true;
                WhisperModel.checkModelAndStart();
                TelegrammConvertOggToWAV.createWAVofOGG();
                for (Map.Entry<File, String> files : ProcessingFile.TranslateVoice().entrySet()) {
                    parseWAVtoResText(files.getKey(), new StringBuilder(files.getValue()));
                }
            }
            case "2": {
                break;
            }
        }
    }

    private void promptForDirectoryPath() {

        while(true) {
            logger.info("Введите полный путь к папке с файлами экспорта:\n");
            if (scanner.hasNextLine()) {
                File inputFile = new File(scanner.nextLine());
                if (inputFile.isDirectory()) {
                    dirMess = inputFile;
                 break;
                } else {
                    logger.warn("Предупреждение: Указанный путь {} не является папкой. Попробуйте снова.\n", inputFile.getAbsolutePath());
                }
            }
        }
    }

    private void scanFiles() {
        File[] allFiles = dirMess.listFiles();
        if (allFiles == null) {
            logger.error("Не удалось прочитать содержимое папки: {}", dirMess.getAbsolutePath());
            return;
        }
        for (File file : allFiles) {
            if (file.isFile()) {
                if (file.getName().toLowerCase().endsWith(".html")) {
                    this.htmlFiles.add(file);
                    logger.info("Найден HTML файл: {}", file.getName());
                } else if (file.getName().equals("result.txt")) {
                    this.transcriptFile = file;
                    logger.info("Найден файл транскрипций: {}", file.getName());
                } else if (file.getName().equals("custom_transcripts.txt")) {
                    this.customVoiceFile = file;
                    logger.info("Найден файл доп транскрипций: {}", file.getName());
                }
            }
        }

        if (this.htmlFiles.isEmpty()) {
            logger.error("Ошибка: В папке не найдены html файлы.");
        } else if (this.transcriptFile == null) {
            logger.warn("Предупреждение: В папке не найден result.txt. Продолжить без транскрипта?\n 1 - да \n 2 - нет");
            String userInput = scanner.nextLine().strip();
            if (userInput.equals("1")) {
                this.transcriptFile = null;
                logger.warn("Предупреждение: Парсер запущен без транскрипта!");
            } else if (userInput.equals("2")) {
                throw new RuntimeException("Ошибка: Пожалуйста, добавьте файл и затем запустите программу повторно!");
            }
        }

        htmlFiles.sort(getNumericComparator.get());
        logger.info("HTML файлы отсортированы для обработки.");
    }

    private void parseTranscriptReader(BufferedReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                int colonIndex = line.indexOf(": ");
                if (colonIndex == -1) {
                    continue;
                }

                int keyName = line.indexOf(" ");
                String key;
                if (!checkModelSet) {
                    key = "voice_messages/" + line.substring(0, keyName) + ".ogg";
                } else {
                    key = "voice_messages/" + line.substring(0, colonIndex);
                }
                String value;
                if (keyName < colonIndex) {
                    if (voiceMess.containsKey(key)) {
                        String temp = voiceMess.get(key);
                        value = temp + line.substring(colonIndex + 2);
                    } else {
                        value = line.substring(colonIndex + 2);
                    }
                } else {
                    value = line.substring(keyName + 2);
                }
                voiceMess.put(key, value);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void parseTranscriptFile() {
        if (transcriptFile == null) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(transcriptFile))) {
            parseTranscriptReader(reader);
        } catch (IOException e) {
            logger.error("Ошибка чтения основного файла транскрипций", e);
        }

        if (customVoiceFile == null) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(customVoiceFile))) {
            parseTranscriptReader(reader);
        } catch (IOException e) {
            logger.error("Ошибка чтения кастомного файла транскрипций", e);
        }
    }

    private void parseHtmlFiles() {
        String lastAuthor = "Неизвестный";
        try {
        for (File htmlFile : htmlFiles) {
            Document htmlTG =  Jsoup.parse(htmlFile, "utf-8");

            Elements messageAll = htmlTG.select("div.message.default");

            for (Element message : messageAll) {
                String currAuthor;

                Element author = message.selectFirst(".from_name");

                if (author != null) {
                    currAuthor = author.text();
                    lastAuthor = currAuthor;
                } else  {
                    currAuthor = lastAuthor;
                }


                Element textElement = message.selectFirst(".text");
                Element voiceMessage = message.selectFirst("a.media_voice_message");
                String text = "ДАННОЕ СООБЩЕНИЕ НЕ СОДЕРЖИТ ТЕКСТ/ГС";

                if (this.transcriptFile != null && voiceMessage != null) {
                    text = voiceMess.get(voiceMessage.attr("href"));
                } else {
                    if (textElement != null) {
                        text = textElement.text();
                    }
                }

                Element dataElement = message.selectFirst(".date");
                String dateStr = "";
                if (dataElement != null) {
                    dateStr = dataElement.attr("title");
                }

                LocalDateTime date = null;
                if (!dateStr.isEmpty()) {
                    date = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss VV"));
                } else {
                    logger.warn("Предупреждение: Время для сообщения после {} - не обнаружено!", allMessages.get(messageAll.size() - 1).timestamp());
                }

                allMessages.add(new Message(currAuthor, date, text));
                }

            }
        } catch (IOException e) {
            logger.error("Ошибка парсинга выходных данных!");
            throw new RuntimeException(e);
        }
    }

    private void parseToOutput() {
        File parseOutput = new File(dirMess.getAbsolutePath());

        logger.info("В каком формате сохранить результат? 1 - TXT, 2 - JSON");
        switch (scanner.nextLine().trim()) {
            case "1": {
                OutputFormatter outputFormatter = new TxtOutputFormatter();
                try {
                    outputFormatter.write(allMessages, parseOutput);
                } catch (IOException e) {
                    logger.error("Не удалось записать файл TXT по пути {}", parseOutput.getAbsolutePath());
                    throw new RuntimeException(e);
                }
                break;
            }
            case "2": {
                OutputFormatter outputFormatter = new JsonOutputFormatter();
                try {
                    outputFormatter.write(allMessages, parseOutput);
                } catch (IOException e) {
                    logger.error("Не удалось записать файл JSON по пути {}", parseOutput.getAbsolutePath());
                    throw new RuntimeException(e);
                }
            }
            default: {
                logger.error("Введено неверное значение выбора");
                parseToOutput();
            }
        }
    }

    private static void parseWAVtoResText(File wavFile, StringBuilder transcribedText) {
        try {
            String resultFileName = wavFile.getName().replace(".wav", ".ogg");

            String outputLine = resultFileName + ": " + transcribedText;
            int indexOut = outputLine.indexOf(":");

            Path resultPath = Paths.get(getDirMess().getAbsolutePath(), "result.txt");

            Set<String> checkStr = new HashSet<>();
            if (Files.exists(resultPath)) {
                try (BufferedReader reader = Files.newBufferedReader(resultPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int indexLine = line.indexOf(":");
                        checkStr.add(line.strip().substring(indexLine));
                    }
                }
            }


            if (!checkStr.contains(outputLine.substring(indexOut))) {
                Files.writeString(resultPath, outputLine + System.lineSeparator(),
                        StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

