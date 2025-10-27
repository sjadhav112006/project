package com.filezipper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class App extends Application {

    private final ZipService zipService = new ZipService();
    private Stage primaryStage;
    private final TextArea logArea = new TextArea();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Button compressFilesButton = new Button("Compress Files");
    private final Button compressFolderButton = new Button("Compress Folder");
    private final Button extractButton = new Button("Extract ZIP");

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("File Compressor and Decompressor");

        // --- Layout ---
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        // --- Button Grid ---
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("grid-pane");

        compressFilesButton.setMaxWidth(Double.MAX_VALUE);
        compressFolderButton.setMaxWidth(Double.MAX_VALUE);
        extractButton.setMaxWidth(Double.MAX_VALUE);

        grid.add(compressFilesButton, 0, 0);
        grid.add(compressFolderButton, 1, 0);
        grid.add(extractButton, 2, 0);

        // --- Progress Bar ---
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-bar");

        // --- Log Area ---
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("text-area");
        VBox.setVgrow(logArea, Priority.ALWAYS); // Make log area expand

        // --- Set Actions ---
        compressFilesButton.setOnAction(e -> handleCompressFiles());
        compressFolderButton.setOnAction(e -> handleCompressFolder());
        extractButton.setOnAction(e -> handleExtract());

        // --- Add to Root ---
        root.getChildren().addAll(grid, progressBar, new Label("Log:"), logArea);

        // --- Scene and Stage ---
        Scene scene = new Scene(root, 600, 400);
        try {
            String cssPath = getClass().getResource("style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            log("Error loading CSS: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.setMinHeight(300);
        primaryStage.setMinWidth(450);
        primaryStage.show();
    }

    private void handleCompressFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Compress");
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);

        if (files == null || files.isEmpty()) {
            log("File selection cancelled.");
            return;
        }

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save ZIP File As");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archives", "*.zip"));
        File outputFile = saveChooser.showSaveDialog(primaryStage);

        if (outputFile == null) {
            log("Save operation cancelled.");
            return;
        }

        // Run as a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                setControlsDisabled(true);
                zipService.compressFiles(files, outputFile,
                        (progress) -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        (message) -> Platform.runLater(() -> log(message))
                );
                return null;
            }
        };

        task.setOnSucceeded(e -> setControlsDisabled(false));
        task.setOnFailed(e -> {
            log("Error during compression: " + e.getSource().getException().getMessage());
            setControlsDisabled(false);
        });

        new Thread(task).start();
    }

    private void handleCompressFolder() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Folder to Compress");
        File folder = dirChooser.showDialog(primaryStage);

        if (folder == null) {
            log("Folder selection cancelled.");
            return;
        }

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Save ZIP File As");
        saveChooser.setInitialFileName(folder.getName() + ".zip");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archives", "*.zip"));
        File outputFile = saveChooser.showSaveDialog(primaryStage);

        if (outputFile == null) {
            log("Save operation cancelled.");
            return;
        }

        // Run as a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                setControlsDisabled(true);
                zipService.compressFolder(folder, outputFile,
                        (progress) -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        (message) -> Platform.runLater(() -> log(message))
                );
                return null;
            }
        };

        task.setOnSucceeded(e -> setControlsDisabled(false));
        task.setOnFailed(e -> {
            log("Error during folder compression: " + e.getSource().getException().getMessage());
            setControlsDisabled(false);
        });

        new Thread(task).start();
    }

    private void handleExtract() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select ZIP to Extract");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Archives", "*.zip"));
        File zipFile = fileChooser.showOpenDialog(primaryStage);

        if (zipFile == null) {
            log("ZIP file selection cancelled.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Extraction Directory");
        File outputDir = dirChooser.showDialog(primaryStage);

        if (outputDir == null) {
            log("Extraction directory selection cancelled.");
            return;
        }

        // Run as a background task
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                setControlsDisabled(true);
                zipService.extractZip(zipFile, outputDir,
                        (progress) -> Platform.runLater(() -> progressBar.setProgress(progress)),
                        (message) -> Platform.runLater(() -> log(message))
                );
                return null;
            }
        };

        task.setOnSucceeded(e -> setControlsDisabled(false));
        task.setOnFailed(e -> {
            log("Error during extraction: " + e.getSource().getException().getMessage());
            setControlsDisabled(false);
        });

        new Thread(task).start();
    }

    // --- Helper Methods ---

    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    private void setControlsDisabled(boolean disabled) {
        compressFilesButton.setDisable(disabled);
        compressFolderButton.setDisable(disabled);
        extractButton.setDisable(disabled);
        if (!disabled) {
            progressBar.setProgress(0); // Reset progress when done
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
