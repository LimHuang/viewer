package com.syspilot.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.controller.MainWindowController;
import com.syspilot.viewer.model.StateData;
import com.syspilot.viewer.system.TrajectorySystem;
import com.syspilot.viewer.utility.PlatformPaths;
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
import java.util.List;

public class App extends Application {

    private static final Path STATE_DIR = PlatformPaths.getSysPilotDir().resolve(".state");
    private static final Path STATE_FILE = STATE_DIR.resolve("state.json");

    // CLI arguments (parsed before JavaFX starts)
    private static String cliLogsDir;
    private static String cliSessionId;

    private MainWindowController controller;

    @Override
    public void start(Stage stage) throws Exception {
        AppArchitecture arch = AppArchitecture.getInstance();
        arch.registerSystem(new TrajectorySystem());
        arch.registerUtility(new TrajectoryLoader());
        arch.init();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 900);
        scene.getStylesheets().add(getClass().getResource("light.css").toExternalForm());
        controller = loader.getController();

        stage.setTitle("SysPilot Trajectory Viewer");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        // If --logs-dir was provided, start session watching mode
        if (cliLogsDir != null && !cliLogsDir.isEmpty()) {
            Path logsPath = Paths.get(cliLogsDir);
            if (Files.isDirectory(logsPath)) {
                controller.startSessionMode(logsPath, cliSessionId);
            }
        }

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

    /**
     * Parse command-line arguments before JavaFX launches.
     * Usage: --logs-dir <path> [--session <id>]
     */
    public static void main(String[] args) {
        // Parse CLI args
        List<String> javaArgs = new java.util.ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--logs-dir" -> {
                    if (i + 1 < args.length) cliLogsDir = args[++i];
                }
                case "--session" -> {
                    if (i + 1 < args.length) cliSessionId = args[++i];
                }
                default -> javaArgs.add(args[i]);
            }
        }
        launch(javaArgs.toArray(new String[0]));
    }
}
