package com.lev666;

import org.slf4j.LoggerFactory;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;


import java.io.File;

public class TelegrammConvertOggToWAV {
    static final org.slf4j.Logger logger = LoggerFactory.getLogger(TelegrammConvertOggToWAV.class);
    private static final String abslPath = DataParser.getDirMess().getAbsolutePath() + "/voice_messages";

    private static final File dirName = new File(abslPath);

    public static void createWAVofOGG() {
        if (dirName.isDirectory()) {
            File[] files = dirName.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".ogg")) {

                        File target = new File(dirName, "WAV/" + file.getName().replace(".ogg", ".wav"));

                        target.getParentFile().mkdirs();

                        try {
                            String pathToFFmpeg = "/usr/bin/ffmpeg";

                            Encoder encoder = new Encoder(new FFMPEGLocator() {
                                @Override
                                public String getFFMPEGExecutablePath() {
                                    return pathToFFmpeg;
                                }
                            });

                            AudioAttributes audio = new AudioAttributes();
                            audio.setCodec("pcm_s16le");
                            audio.setChannels(1);
                            audio.setSamplingRate(16000);

                            EncodingAttributes attrs = new EncodingAttributes();
                            attrs.setOutputFormat("wav");
                            attrs.setAudioAttributes(audio);

                            Encoder encoder = new Encoder();
                            encoder.encode(new MultimediaObject(file), target, attrs);

                            logger.info("Файл {} успешно конвертирован.", file.getName());

                        } catch (Exception e) {
                            logger.error("Не удалось конвертировать файл: {}", file.getName(), e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}