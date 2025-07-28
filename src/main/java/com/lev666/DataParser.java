package com.lev666;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataParser {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(DataParser.class);
    Scanner scanner = new Scanner(System.in);
    Map<String, String> voiceMess = new HashMap<>();
    List<Message> allMessages = new ArrayList<>();

    File dirMess;
    private final List<File> htmlFiles = new ArrayList<>();
    private File transcriptFile;

    public void startParsing() {
        promptForDirectoryPath();

        logger.info("Парсинг будет происходить для папки: {}", dirMess.getAbsolutePath());

        scanFiles();
        parseTranscriptFile();
        parseHtmlFiles();
        parseToOutput();

        logger.info("Парсинг завершён.");
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

    public static Comparator<File> getNumericComparator() {
        return (f1, f2) -> {
            String name1 = f1.getName();
            String name2 = f2.getName();

            String numStr1 = name1.replaceAll("\\D", "");
            String numStr2 = name2.replaceAll("\\D", "");

            if (numStr1.isEmpty() && numStr2.isEmpty()) {
                return name1.compareTo(name2);
            } else if (numStr1.isEmpty()) {
                return -1;
            } else if (numStr2.isEmpty()) {
                return 1;
            }

            try {
                int num1 = Integer.parseInt(numStr1);
                int num2 = Integer.parseInt(numStr2);

                return Integer.compare(num1, num2);
            } catch (NumberFormatException e) {
                return name1.compareTo(name2);
            }
        };
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
                }
            }
        }

        if (this.htmlFiles.isEmpty()) {
            logger.error("Ошибка: В папке не найдены html файлы.");
        } else if (this.transcriptFile == null) {
            logger.warn("Предупреждение: В папке не найден result.txt. Продолжить без транскрипта?\n 1 - да \n 2 - нет");
            String userInput = scanner.nextLine().trim();
            if (userInput.equals("1")) {
                this.transcriptFile = null;
                logger.warn("Предупреждение: Парсер запущен бещ транскрипта!");
            } else if (userInput.equals("2")) {
                throw new RuntimeException("Ошибка: Пожалуйста, добавьте файл и затем запустите программу повторно!");
            }
        }

        htmlFiles.sort(getNumericComparator());
        logger.info("HTML файлы отсортированы для обработки.");
    }

    private void parseTranscriptFile() {
        if (this.transcriptFile != null) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(this.transcriptFile))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    int colonIndex = line.indexOf(": ");
                    int keyName = line.indexOf(" ");
                    if (colonIndex != -1) {
                        String key = "voice_messages/" + line.substring(0, keyName) + ".ogg";
                        String value;
                        if (voiceMess.containsKey(key)) {
                            String temp = voiceMess.get(key);
                            value = temp + line.substring(colonIndex + 2);
                        } else {
                            value = line.substring(colonIndex + 2);
                        }

                        voiceMess.put(key, value);
                    }
                }
            } catch (FileNotFoundException e) {
                logger.error("Файл с транскриптом ГС не найден!");
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.error("Ошибка чтения транскрипта ГС {}!", this.transcriptFile.getAbsolutePath());
                throw new RuntimeException(e);
            }
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
        }
    }
}

