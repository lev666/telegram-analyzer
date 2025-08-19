package com.lev666;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;


public class JsonOutputFormatter implements OutputFormatter {
    final org.slf4j.Logger logger = LoggerFactory.getLogger(JsonOutputFormatter.class);
    @Override
    public void write(List<Message> messages, File directory,  GUIParamConfig GUIParamConfig){
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        String json = gson.toJson(messages);

        try {
            Path outputPath = directory.toPath().resolve("OutputParse.json");
            Files.writeString(outputPath, json);
        } catch (IOException e) {
            if (GUIParamConfig.useGUI()) {
                GUIParamConfig.guiReporter().report("Ошибка записи в " + directory.getAbsolutePath());
            }
            logger.error("Ошибка записи в {}", directory.getAbsolutePath());
        }
    }
}
