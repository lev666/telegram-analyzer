package com.lev666;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WhisperModel {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(WhisperModel.class);

    private static final String directoryPath = System.getProperty("user.dir");
    private static final String FILE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin";
    private static final String fileName = "ggml-large-v3-turbo.bin";
    private static final File directory = new File(directoryPath);

    private static final File file = new File(directory, fileName);

    public static void checkModelAndStart() {
        if (!file.exists()) {
            downloadModelWithProgress();
        } else {
            logger.info("Модель уже скачана -> пропуск загрузки.");
        }
    }

    public static File getFile() {
        return file;
    }

    public static void downloadModelWithProgress() {
        Path outputPath = Paths.get(fileName);

        try {
            System.out.println("Подключение к серверу...");
            // Сразу открываем поток, без предварительной проверки размера
            try (InputStream in = new URL(FILE_URL).openStream();
                 FileOutputStream out = new FileOutputStream(outputPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                char[] spinner = {'|', '/', '-', '\\'};
                int spinnerIndex = 0;

                logger.info("Начинается скачивание модели...");

                // Главный цикл скачивания
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if ((spinnerIndex % 128) == 0) {
                        System.out.print("\rСкачано: " + (totalBytesRead / 1024 / 1024) + " MB " + spinner[(spinnerIndex / 128) % 4]);
                    }
                    spinnerIndex++;
                }

                System.out.println();

                logger.info("\nМодель успешно скачана: {}", outputPath.toAbsolutePath());

            }
        } catch (IOException e) {
            logger.error("Ошибка при скачивании файла: {}", e.getMessage());
        }
    }
}
