package com.syspilot.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.controller.MainWindowController;
import com.syspilot.viewer.model.StateData;
import com.syspilot.viewer.system.TrajectorySystem;
import com.syspilot.viewer.utility.TrajectoryLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App extends Application {

    private static final Path STATE_DIR = Paths.get(System.getProperty("user.home"), ".syspilot");
    private static final Path STATE_FILE = STATE_DIR.resolve("state.json");

    private MainWindowController controller;

    @Override
    public void start(Stage stage) throws Exception {
        AppArchitecture arch = AppArchitecture.getInstance();
        arch.registerSystem(new TrajectorySystem());
        arch.registerUtility(new TrajectoryLoader());
        arch.init();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 900);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        controller = loader.getController();

        stage.setTitle("SysPilot Trajectory Viewer");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        // Restore previous session
        restoreState();
    }

    @Override
    public void stop() {
        saveState();
        AppArchitecture.getInstance().deinit();
    }

    private void saveState() {
        try {
            TrajectorySystem system = AppArchitecture.getInstance().getSystem(TrajectorySystem.class);
            StateData state = new StateData();
            state.setOpenFiles(new java.util.ArrayList<>(system.getOpenFiles()));
            state.setActiveFile(system.getActiveKey());
            Files.createDirectories(STATE_DIR);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(STATE_FILE.toFile(), state);
        } catch (IOException e) {
            System.err.println("Failed to save state: " + e.getMessage());
        }
    }

    private void restoreState() {
        File file = STATE_FILE.toFile();
        if (!file.exists()) return;
        try {
            StateData state = new ObjectMapper().readValue(file, StateData.class);
            if (state.getOpenFiles() != null && !state.getOpenFiles().isEmpty()) {
                controller.restoreState(state);
            }
        } catch (IOException e) {
            System.err.println("Failed to restore state: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
