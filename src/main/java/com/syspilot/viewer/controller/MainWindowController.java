package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.command.LoadTrajectoryCommand;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.*;
import com.syspilot.viewer.utility.PlatformPaths;
import com.syspilot.viewer.service.LogsWatcherService;
import com.syspilot.viewer.system.TrajectorySystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MainWindowController extends BaseController {

    private enum ViewMode { MAIN, CHARTS, SUBAGENTS }

    @FXML private VBox errorBar;
    @FXML private Text errorTitleText;
    @FXML private Text errorDetailText;

    @FXML private HBox statsBar;
    @FXML private Text modelText;
    @FXML private Text stepsText;
    @FXML private Text timeText;
    @FXML private Text tokensInText;
    @FXML private Text tokensOutText;
    @FXML private Text filePathText;
    @FXML private Text statusText;
    @FXML private StackPane contentStack;
    @FXML private Node mainSplit;
    @FXML private TabPane tabPane;
    @FXML private Button backButton;
    @FXML private VBox sessionTreeArea;
    @FXML private TreeView<String> sessionTree;

    @FXML private StepDetailPanelController stepDetailController;

    private Node chartView;
    private Node subAgentView;
    private TrajectoryData currentTrajectory;
    private ViewMode currentViewMode = ViewMode.MAIN;
    private boolean darkMode = false;
    private LogsWatcherService logsWatcher;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());
        registerEvent(TrajectoryLoadedEvent.class, this::onTrajectoryLoaded);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> {
            if (tab != null && tab.getUserData() != null) {
                String key = (String) tab.getUserData();
                TrajectorySystem system = getSystem(TrajectorySystem.class);
                if (!key.equals(system.getActiveKey())) {
                    system.switchTo(key);
                }
            }
        });
    }

    @FXML
    private void onOpenFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open trajectory.json");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showOpenDialog(contentStack.getScene().getWindow());
        if (file != null) {
            String path = file.getAbsolutePath();
            TrajectorySystem system = getSystem(TrajectorySystem.class);

            if (system.getOpenFiles().contains(path)) {
                for (Tab tab : tabPane.getTabs()) {
                    if (path.equals(tab.getUserData())) {
                        tabPane.getSelectionModel().select(tab);
                        break;
                    }
                }
                statusText.setText("Switched to: " + path);
                return;
            }

            try {
                statusText.setText("Loading: " + path + "...");
                sendCommand(new LoadTrajectoryCommand(file));
                statusText.setText("Loaded: " + path);
                addTabForFile(path, system);
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to load: " + e.getMessage()).show();
                statusText.setText("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onOpenFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Folder with trajectory JSON files");
        File dir = chooser.showDialog(contentStack.getScene().getWindow());
        if (dir == null) return;

        List<File> jsonFiles = new ArrayList<>();
        collectJsonFiles(dir, jsonFiles);

        if (jsonFiles.isEmpty()) {
            statusText.setText("No JSON files found in: " + dir.getName());
            return;
        }

        jsonFiles.sort(java.util.Comparator.comparing(File::getName));
        statusText.setText("Loading " + jsonFiles.size() + " files from " + dir.getName() + "...");

        int loaded = 0;
        int skipped = 0;
        for (File file : jsonFiles) {
            String path = file.getAbsolutePath();
            TrajectorySystem system = getSystem(TrajectorySystem.class);
            if (system.getOpenFiles().contains(path)) continue;

            try {
                sendCommand(new LoadTrajectoryCommand(file));
                addTabForFile(path, system);
                loaded++;
            } catch (Exception e) {
                if (!e.getMessage().contains("Not a trajectory file")) {
                    System.err.println("Failed to load: " + path + " - " + e.getMessage());
                }
                skipped++;
            }
        }
        if (skipped > 0) {
            statusText.setText("Loaded " + loaded + " files, skipped " + skipped + " non-trajectory files");
        } else {
            statusText.setText("Loaded " + loaded + " files from " + dir.getName());
        }
    }

    private void collectJsonFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectJsonFiles(file, result);
            } else if (file.getName().toLowerCase().endsWith(".json")) {
                result.add(file);
            }
        }
    }

    private void addTabForFile(String path, TrajectorySystem system) {
        String fileName = new File(path).getName();
        Tab tab = new Tab(fileName);
        tab.setUserData(path);
        tab.setTooltip(new Tooltip(path));
        tab.setOnCloseRequest(e -> {
            system.removeTrajectory(path);
            if (system.isEmpty()) {
                showEmptyState();
            }
        });
        tabPane.setVisible(true);
        tabPane.setManaged(true);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    @FXML
    private void onThemeDark() {
        darkMode = true;
        Scene scene = contentStack.getScene();
        if (scene != null) {
            scene.getStylesheets().setAll(
                    getClass().getResource("/com/syspilot/viewer/style.css").toExternalForm());
        }
        if (stepDetailController != null) {
            stepDetailController.setDarkMode(true);
        }
    }

    @FXML
    private void onThemeLight() {
        darkMode = false;
        Scene scene = contentStack.getScene();
        if (scene != null) {
            scene.getStylesheets().setAll(
                    getClass().getResource("/com/syspilot/viewer/light.css").toExternalForm());
        }
        if (stepDetailController != null) {
            stepDetailController.setDarkMode(false);
        }
    }

    @FXML
    private void onShowCharts() {
        if (currentTrajectory == null) return;
        if (chartView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syspilot/viewer/stats_chart_panel.fxml"));
                chartView = loader.load();
            } catch (IOException e) {
                statusText.setText("Failed to load charts: " + e.getMessage());
                return;
            }
        }
        currentViewMode = ViewMode.CHARTS;
        contentStack.getChildren().setAll(chartView);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }

    @FXML
    private void onShowSubAgents() {
        if (currentTrajectory == null) return;
        if (subAgentView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/syspilot/viewer/subagent_tree_panel.fxml"));
                subAgentView = loader.load();
            } catch (IOException e) {
                statusText.setText("Failed to load sub-agents: " + e.getMessage());
                return;
            }
        }
        currentViewMode = ViewMode.SUBAGENTS;
        contentStack.getChildren().setAll(subAgentView);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }

    @FXML
    private void onBackToMain() {
        currentViewMode = ViewMode.MAIN;
        contentStack.getChildren().setAll(mainSplit);
        backButton.setVisible(false);
        backButton.setManaged(false);
    }

    @FXML
    private void onRefreshTree() {
        if (logsWatcher == null) return;
        logsWatcher.refreshNow();
        TreeItem<String> root = sessionTree.getRoot();
        if (root == null) return;
        refreshSessionTree(root);
        statusText.setText("Tree refreshed");
    }

    // ---- Session mode (--logs-dir) ----

    public void startSessionMode(Path logsDir, String activeSessionId) {
        try {
            logsWatcher = new LogsWatcherService(logsDir);
            logsWatcher.start();
        } catch (IOException e) {
            statusText.setText("Failed to watch logs directory: " + e.getMessage());
            return;
        }

        // Build session tree
        sessionTreeArea.setVisible(true);
        sessionTreeArea.setManaged(true);
        sessionTree.setShowRoot(false);
        TreeItem<String> root = new TreeItem<>("Sessions");
        refreshSessionTree(root);

        // Listen for new turns in real-time
        logsWatcher.newTurnProperty().addListener((obs, old, turn) -> {
            if (turn != null) {
                Platform.runLater(() -> {
                    refreshSessionTree(root);
                    // Auto-load the new turn
                    loadTurnForPath(turn.getTrajectoryFile().toString());
                });
            }
        });

        sessionTree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                loadTurnForItem(item);
            }
        });

        sessionTree.setRoot(root);

        // Auto-select the most recent session's latest turn
        if (!root.getChildren().isEmpty()) {
            TreeItem<String> targetSession = null;
            if (activeSessionId != null) {
                for (TreeItem<String> child : root.getChildren()) {
                    if (child.getValue().equals(activeSessionId)) {
                        targetSession = child;
                        break;
                    }
                }
            }
            if (targetSession == null) {
                targetSession = root.getChildren().get(root.getChildren().size() - 1);
            }
            if (!targetSession.getChildren().isEmpty()) {
                TreeItem<String> lastTurn = targetSession.getChildren()
                        .get(targetSession.getChildren().size() - 1);
                sessionTree.getSelectionModel().select(lastTurn);
                sessionTree.scrollTo(sessionTree.getRow(lastTurn));
            }
        }

        // Auto-select the active session's latest turn (or most recent session)
        if (activeSessionId != null) {
            statusText.setText("Watching: " + logsDir + " (session: " + activeSessionId + ")");
        } else {
            statusText.setText("Watching: " + logsDir);
        }
    }

    // Map tree items to their trajectory file paths for loading
    private final java.util.Map<TreeItem<String>, String> treeItemPaths = new java.util.HashMap<>();

    private void refreshSessionTree(TreeItem<String> root) {
        root.getChildren().clear();
        treeItemPaths.clear();
        for (SessionData session : logsWatcher.getSessions()) {
            TreeItem<String> sessionItem = new TreeItem<>(session.getName());
            for (TurnData turn : session.getTurns()) {
                String label = turn.getName();
                if (turn.getPreview() != null && !turn.getPreview().isEmpty()) {
                    label += " — " + turn.getPreview();
                }
                TreeItem<String> turnItem = new TreeItem<>(label);
                treeItemPaths.put(turnItem, turn.getTrajectoryFile().toString());
                sessionItem.getChildren().add(turnItem);
            }
            root.getChildren().add(sessionItem);
            sessionItem.setExpanded(true);
        }
    }

    private void loadTurnForPath(String path) {
        File file = new File(path);
        if (!file.exists()) return;

        TrajectorySystem system = getSystem(TrajectorySystem.class);
        if (system.getOpenFiles().contains(path)) {
            for (Tab tab : tabPane.getTabs()) {
                if (path.equals(tab.getUserData())) {
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }

        try {
            statusText.setText("Loading new turn...");
            sendCommand(new LoadTrajectoryCommand(file));
            statusText.setText("Loaded new turn");
            addTabForFile(path, system);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }

    private void loadTurnForItem(TreeItem<String> item) {
        String path = treeItemPaths.get(item);
        if (path == null) return;
        File file = new File(path);
        if (!file.exists()) return;

        TrajectorySystem system = getSystem(TrajectorySystem.class);

        // If already open, switch to its tab
        if (system.getOpenFiles().contains(path)) {
            for (Tab tab : tabPane.getTabs()) {
                if (path.equals(tab.getUserData())) {
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }

        try {
            statusText.setText("Loading: " + file.getParentFile().getName() + "/" + file.getName() + "...");
            sendCommand(new LoadTrajectoryCommand(file));
            statusText.setText("Loaded: " + item.getValue());
            addTabForFile(path, system);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }

    private void showEmptyState() {
        errorBar.setVisible(false);
        errorBar.setManaged(false);
        statsBar.setVisible(false);
        statsBar.setManaged(false);
        tabPane.setVisible(false);
        tabPane.setManaged(false);
        filePathText.setText("");
        contentStack.getChildren().setAll(mainSplit);
        currentTrajectory = null;
        currentViewMode = ViewMode.MAIN;
        statusText.setText("Ready — Open a trajectory.json file");
    }

    public void restoreState(StateData state) {
        TrajectorySystem system = getSystem(TrajectorySystem.class);
        List<String> validPaths = new ArrayList<>();
        for (String path : state.getOpenFiles()) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("Skipping non-existent file: " + path);
                continue;
            }
            try {
                sendCommand(new LoadTrajectoryCommand(file));
                addTabForFile(path, system);
                validPaths.add(path);
            } catch (Exception e) {
                System.err.println("Failed to restore: " + path + " - " + e.getMessage());
            }
        }
        // Clean up state by removing non-existent paths
        if (validPaths.size() != state.getOpenFiles().size()) {
            state.setOpenFiles(validPaths);
            if (state.getActiveFile() != null && !validPaths.contains(state.getActiveFile())) {
                state.setActiveFile(validPaths.isEmpty() ? null : validPaths.get(validPaths.size() - 1));
            }
            // Persist cleaned state
            try {
                java.nio.file.Path stateDir = PlatformPaths.getSysPilotDir().resolve(".state");
                java.nio.file.Files.createDirectories(stateDir);
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(stateDir.resolve("state.json").toFile(), state);
            } catch (IOException e) {
                System.err.println("Failed to save cleaned state: " + e.getMessage());
            }
        }
        // Select active tab
        if (state.getActiveFile() != null && validPaths.contains(state.getActiveFile())) {
            for (Tab tab : tabPane.getTabs()) {
                if (state.getActiveFile().equals(tab.getUserData())) {
                    tabPane.getSelectionModel().select(tab);
                    break;
                }
            }
        }
    }

    private void onTrajectoryLoaded(TrajectoryLoadedEvent event) {
        currentTrajectory = event.getTrajectory();
        if (currentTrajectory == null) {
            showEmptyState();
            return;
        }
        TrajectoryData t = currentTrajectory;
        SummaryData s = t.getSummary();

        // File path
        TrajectorySystem system = getSystem(TrajectorySystem.class);
        filePathText.setText(system.getActiveKey() != null ? system.getActiveKey() : "");

        // Model name
        String model = findModelName(t);
        modelText.setText(model);

        // Steps
        int total = s != null ? s.getTotalSteps() : (t.getSteps() != null ? t.getSteps().size() : 0);
        int subCount = s != null ? s.getSubagentCount() : 0;
        stepsText.setText(total + (subCount > 0 ? " (+" + subCount + " subagent)" : ""));

        // Time
        double secs = t.getExecutionTimeSeconds();
        if (secs >= 60) {
            long min = (long) secs / 60;
            long sec = (long) secs % 60;
            timeText.setText(String.format("%dm %ds", min, sec));
        } else {
            timeText.setText(String.format("%.1fs", secs));
        }

        // Tokens
        int in = s != null ? s.getTotalTokensIn() : 0;
        int out = s != null ? s.getTotalTokensOut() : 0;
        tokensInText.setText(formatLargeNum(in));
        tokensOutText.setText(formatLargeNum(out));

        statsBar.setVisible(true);
        statsBar.setManaged(true);

        // Error bar — show trajectory-level and summary errors
        List<String> allErrors = new ArrayList<>();
        if (!t.isSuccess() && t.getError() != null && !t.getError().isEmpty()) {
            allErrors.add(t.getError());
        }
        if (s != null && s.getErrors() != null) {
            allErrors.addAll(s.getErrors());
        }
        if (!allErrors.isEmpty()) {
            errorTitleText.setText("Error: " + allErrors.get(0));
            if (allErrors.size() > 1) {
                errorDetailText.setText(String.join("\n", allErrors));
                errorDetailText.setVisible(true);
                errorDetailText.setManaged(true);
            } else {
                errorDetailText.setVisible(false);
                errorDetailText.setManaged(false);
            }
            errorBar.setVisible(true);
            errorBar.setManaged(true);
        } else {
            errorBar.setVisible(false);
            errorBar.setManaged(false);
        }

        // Restore appropriate view
        switch (currentViewMode) {
            case CHARTS -> {
                contentStack.getChildren().setAll(chartView != null ? chartView : mainSplit);
                backButton.setVisible(true);
                backButton.setManaged(true);
            }
            case SUBAGENTS -> {
                contentStack.getChildren().setAll(subAgentView != null ? subAgentView : mainSplit);
                backButton.setVisible(true);
                backButton.setManaged(true);
            }
            default -> {
                contentStack.getChildren().setAll(mainSplit);
                backButton.setVisible(false);
                backButton.setManaged(false);
            }
        }
    }

    private String findModelName(TrajectoryData t) {
        if (t.getSteps() != null) {
            for (StepData step : t.getSteps()) {
                if (step.getModelInfo() != null && step.getModelInfo().getModelName() != null) {
                    return step.getModelInfo().getModelName();
                }
            }
        }
        return "unknown";
    }

    private static String formatLargeNum(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000.0);
        if (n >= 1000) return String.format("%,d", n);
        return String.valueOf(n);
    }
}
