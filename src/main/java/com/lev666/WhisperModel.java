package com.lev666;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class WhisperModel {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(WhisperModel.class);

    private final String directoryPath = System.getProperty("user.dir");
    private String FILE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/";
    private String fileName = "";
    private final File directory = new File(directoryPath);
    private final Scanner scanner = new Scanner(System.in);
    private GUIParamConfig guiParamConfig;

    private File file;

    private final boolean useGUI;
    private final ProgressReporter guiReporter;

    public WhisperModel(GUIParamConfig GUIParamConfig) {
        this.guiReporter = GUIParamConfig.guiReporter();
        this.useGUI = GUIParamConfig.useGUI();
        this.guiParamConfig = GUIParamConfig;
    }

    public void checkModelAndStart() {
        if (guiParamConfig.modelWithDir() == null) {
            logger.info("Укажите название модели из репозитория ggerganov/whisper.cpp в формате (e.g. `model.bin`)");
            logger.info("По умолчанию модель `ggml-large-v3-turbo.bin`");
            fileName = scanner.nextLine();
            if (fileName.isEmpty()) {
                fileName = "ggml-large-v3-turbo.bin";
            }
            logger.info("Выбрана модель {}", fileName);
        } else {
            fileName = guiParamConfig.modelWithDir();
            guiReporter.report("Выбрана модель " + fileName);
        }
        FILE_URL += fileName;
        file = new File(directory, fileName);

        if (!file.exists()) {
            downloadModelWithProgress();
        } else {
            if (useGUI) {
                guiReporter.report("Модель уже скачана -> пропуск загрузки.");
            }
            logger.info("Модель уже скачана -> пропуск загрузки.");
        }
    }

    public  File getFile() {
        return file;
    }

    public  void downloadModelWithProgress() {
        Path outputPath = Paths.get(fileName);

        try {
            if (useGUI) {
                guiReporter.report("Подключение к серверу...");
            }
            logger.info("Подключение к серверу...\n");
            try (InputStream in = new URL(FILE_URL).openStream();
                 FileOutputStream out = new FileOutputStream(outputPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                char[] spinner = {'|', '/', '-', '\\'};
                int spinnerIndex = 0;

                if (useGUI) {
                    guiReporter.report("Начинается скачивание модели...");
                }
                logger.info("Начинается скачивание модели...");

                while ((bytesRead = in.read(buffer)) != -1) {
                    if (guiParamConfig.task().isCancelled()) {
                        guiReporter.report("Операция отменена пользователем...");
                        return;
                    }
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if ((spinnerIndex % 128) == 0) {
                        String message = "\rСкачано: " + (totalBytesRead / 1024 / 1024) + " MB " + spinner[(spinnerIndex / 128) % 4];
                        if (useGUI) {
                            guiReporter.report(message);
                        }
                        System.out.print(message);
                    }
                    spinnerIndex++;
                }

                System.out.println();

                if (useGUI) {
                    guiReporter.report("Модель успешно скачана: " + outputPath.toAbsolutePath());
                }
                logger.info("\nМодель успешно скачана: {}", outputPath.toAbsolutePath());

            }
        } catch (IOException e) {
            if (useGUI) {
                guiReporter.report("Ошибка при скачивании файла: " + e.getMessage());
            }
            logger.error("Ошибка при скачивании файла: {}", e.getMessage());
        }
    }
}
