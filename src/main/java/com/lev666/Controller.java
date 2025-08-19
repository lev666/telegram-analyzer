package com.lev666;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class Controller {

    @FXML
    private Button folderPathButtom;

    @FXML
    private CheckBox selectAIModel;

    @FXML
    private TextField folderPathField;

    @FXML
    private TextArea logTextArea;

    @FXML
    private Button startButtom;

    @FXML
    private TextField selectAiModelText;

    @FXML
    private TextField selectAiModelLang;

    @FXML
    private VBox aiSettingsPane;

    @FXML
    private Button cancelButton;

    @FXML
    private ComboBox<String> outputFormatBox;



    private GUIParamConfig guiParamConfig;
    private Task<Void> task;
    private ProgressReporter guiReporter;

    @FXML
    public void initialize() {
        aiSettingsPane.visibleProperty().bind(selectAIModel.selectedProperty());
        aiSettingsPane.managedProperty().bind(selectAIModel.selectedProperty());

        selectAIModel.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                if (selectAiModelText.getText().isEmpty()) {
                    selectAiModelText.setText("ggml-large-v3-turbo.bin");
                }
                if (selectAiModelLang.getText().isEmpty()) {
                    selectAiModelLang.setText("ru");
                }
            }
        });

        BooleanBinding selectAIModelEnabled = Bindings.createBooleanBinding(() ->
                        ((selectAIModel.isSelected() && (selectAiModelText.getText().isEmpty() || selectAiModelLang.getText().isEmpty()))
                        || folderPathField.getText().isEmpty()),
                folderPathField.textProperty(), selectAIModel.selectedProperty(),
                selectAiModelLang.textProperty(), selectAiModelText.textProperty());

        startButtom.disableProperty().bind(selectAIModelEnabled);

        outputFormatBox.getItems().addAll("TXT", "JSON");
        outputFormatBox.getSelectionModel().select(0);
    }

    @FXML
    public void folderSelectClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку с экспортом Telegram");
        Stage primaryStage = (Stage) folderPathButtom.getScene().getWindow();

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            folderPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void selectAiModelEnter() {
        String aiModel = selectAiModelText.getText();
        selectAiModelText.setText(aiModel);
    }

    @FXML
    private void selectAiModelLangEnter() {
        String aiModel = selectAiModelLang.getText();
        selectAiModelLang.setText(aiModel);

    }

    @FXML
    public void startClick() {
        task = new Task<>() {
            @Override
            protected Void call() {
                String selectedFormat = outputFormatBox.getSelectionModel().getSelectedItem();

                if (!logTextArea.getText().isEmpty()) {
                    logTextArea.clear();
                }
                DataParser parser = new DataParser();
                guiReporter = this::updateMessage;
                guiParamConfig = new GUIParamConfig(folderPathField.getText(), guiReporter,
                        selectAIModel.isSelected(), selectAiModelText.getText(), selectAiModelLang.getText(), task, selectedFormat);
                parser.startParsingForGUI(guiParamConfig);
                return null;
            }
        };

        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null) {
                logTextArea.appendText(newMsg + "\n");
            }
        });


        Thread thread = new Thread(task);
        thread.start();
        }

        @FXML
        private void onCancelClick() {
            if (task != null && task.isRunning()) {
                task.cancel();
            }
        }
    }