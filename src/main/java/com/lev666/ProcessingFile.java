package com.lev666;

import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ProcessingFile {
    static final Logger logger = LoggerFactory.getLogger(ProcessingFile.class);
    private static final Path modelWithDir = WhisperModel.getFile().toPath();
    private static final String abslPathVoice = DataParser.getDirMess().getAbsolutePath() + "/voice_messages/WAV";
    private static final File wavDir = new File(abslPathVoice);


    public static float[] ReadWavFloat(File wavFile) {
        try {
            byte[] audioInputStream = AudioSystem.getAudioInputStream(wavFile).readAllBytes();
            float[] record = new float[audioInputStream.length / 2];

            for (int i = 0; i < audioInputStream.length / 2; i++) {
                short translateByte = (short) (audioInputStream[i * 2 + 1] << 8 | audioInputStream[i * 2] & 0xFF);
                record[i] = translateByte / 32768.0f;
            }

            return record;
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void TranslateVoice() {
        var whisper = new WhisperJNI();
        try {
            WhisperJNI.loadLibrary();
            var ctx = whisper.init(modelWithDir);
            File[] files = wavDir.listFiles(((dir, name) ->  name.toLowerCase().endsWith(".wav")));
            if (files != null) {
                for (File wavFile : files) {
                    logger.info("Начинаю транскрибацию файла: {}", wavFile.getName());

                    float[] samples = ReadWavFloat(wavFile);

                    var params = new WhisperFullParams();
                    int result = whisper.full(ctx, params, samples, samples.length);
                    if (result != 0) {
                        logger.error("Транскрибация файла {} провалилась с кодом {}", wavFile.getName(), result);
                        continue;
                    }

                    StringBuilder transcribedText = new StringBuilder();
                    int numSegments = whisper.fullNSegments(ctx);
                    for (int i = 0; i < numSegments; i++) {
                        transcribedText.append(whisper.fullGetSegmentText(ctx, i));
                    }

                    String resultFileName = wavFile.getName().replace(".wav", ".ogg");

                    String outputLine = resultFileName + ": " + transcribedText + "\n";

                    Path resultPath = Paths.get(DataParser.getDirMess().getAbsolutePath(), "result.txt");

                    Files.writeString(resultPath, outputLine,
                            StandardOpenOption.APPEND,
                            StandardOpenOption.CREATE);
                }
            }
            ctx.close();
        } catch (IOException e) {
            logger.error("Ошибка записи в result.txt", e);
            throw new RuntimeException(e);
        }
    }
}
