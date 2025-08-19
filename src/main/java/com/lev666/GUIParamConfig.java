package com.lev666;

import javafx.concurrent.Task;

public record GUIParamConfig(String abslPath,
                             ProgressReporter guiReporter,
                             boolean useGUI,
                             String modelWithDir,
                             String langAIGUI,
                             Task<?> task,
                             String outputFormat
                             )
{}
