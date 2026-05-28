package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.command.LoadTrajectoryCommand;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.StepData;
import com.syspilot.viewer.model.SummaryData;
import com.syspilot.viewer.model.TrajectoryData;
import com.syspilot.viewer.system.TrajectorySystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class MainWindowController extends BaseController {

    @FXML private HBox statsBar;
    @FXML private Text modelText;
    @FXML private Text stepsText;
    @FXML private Text timeText;
    @FXML private Text tokensInText;
    @FXML private Text tokensOutText;
    @FXML private Text statusText;
    @FXML private StackPane contentStack;
    @FXML private Node mainSplit;
    @FXML private TabPane tabPane;
    @FXML private Button backButton;

    private Node chartView;
    private Node subAgentView;
    private TrajectoryData currentTrajectory;

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

            // Check if already open — switch to existing tab
            if (system.getOpenFiles().contains(path)) {
                for (Tab tab : tabPane.getTabs()) {
                    if (path.equals(tab.getUserData())) {
                        tabPane.getSelectionModel().select(tab);
                        break;
                    }
                }
                statusText.setText("Switched to: " + file.getName());
                return;
            }

            try {
                statusText.setText("Loading: " + file.getName() + "...");
                sendCommand(new LoadTrajectoryCommand(file));
                statusText.setText("Loaded: " + file.getName());

                // Create tab — the LoadTrajectoryCommand fires TrajectoryLoadedEvent
                // which calls onTrajectoryLoaded to update stats and views
                Tab tab = new Tab(file.getName());
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
        contentStack.getChildren().setAll(subAgentView);
        backButton.setVisible(true);
        backButton.setManaged(true);
    }

    @FXML
    private void onBackToMain() {
        contentStack.getChildren().setAll(mainSplit);
        backButton.setVisible(false);
        backButton.setManaged(false);
    }

    private void showEmptyState() {
        statsBar.setVisible(false);
        statsBar.setManaged(false);
        tabPane.setVisible(false);
        tabPane.setManaged(false);
        contentStack.getChildren().setAll(mainSplit);
        currentTrajectory = null;
        statusText.setText("Ready — Open a trajectory.json file");
    }

    private void onTrajectoryLoaded(TrajectoryLoadedEvent event) {
        currentTrajectory = event.getTrajectory();
        TrajectoryData t = currentTrajectory;
        SummaryData s = t.getSummary();

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

        // Back to main view
        contentStack.getChildren().setAll(mainSplit);
        backButton.setVisible(false);
        backButton.setManaged(false);
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
