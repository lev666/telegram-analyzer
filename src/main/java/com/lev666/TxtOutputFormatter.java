package com.lev666;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.lev666.DataParser.logger;

public class TxtOutputFormatter implements OutputFormatter {
    @Override
    public void write(List<Message> messages, File directory) {
        List<String> strMessages = new ArrayList<>();
        for (Message message : messages) {
            String formattedLine = String.format("[%s] %s: %s", message.timestamp().toString(), message.author(), message.text());
            strMessages.add(formattedLine);
        }

        try {
            Files.write(Path.of(directory.toPath() + "/OutputParse.txt"), strMessages);
        } catch (IOException e) {
            logger.error("Ошибка записи в {}", directory.getAbsolutePath());
        }
    }
}
