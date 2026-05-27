package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.StepData;
import com.syspilot.viewer.model.ToolCallData;
import com.syspilot.viewer.model.TrajectoryData;
import com.syspilot.viewer.system.TrajectorySystem;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;

import java.util.*;

public class StatsChartPanelController extends BaseController {

    @FXML private LineChart<String, Number> tokenLineChart;
    @FXML private BarChart<String, Number> durationBarChart;
    @FXML private Text emptyText;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());
        registerEvent(TrajectoryLoadedEvent.class, this::onTrajectoryLoaded);

        // Check for already-loaded data
        TrajectoryData existing = getSystem(TrajectorySystem.class).getTrajectory();
        if (existing != null) {
            onTrajectoryLoaded(new TrajectoryLoadedEvent(existing));
        } else {
            showEmpty(true);
        }
    }

    private void onTrajectoryLoaded(TrajectoryLoadedEvent event) {
        showEmpty(false);
        updateTokenLineChart(event.getTrajectory().getSteps());
        updateDurationBarChart(event.getTrajectory().getSteps());
    }

    private void updateTokenLineChart(List<StepData> steps) {
        tokenLineChart.getData().clear();

        XYChart.Series<String, Number> inSeries = new XYChart.Series<>();
        inSeries.setName("Input Tokens");

        XYChart.Series<String, Number> outSeries = new XYChart.Series<>();
        outSeries.setName("Output Tokens");

        for (StepData step : steps) {
            if (step.getModelInfo() != null) {
                String label = "S" + step.getStepId();
                inSeries.getData().add(new XYChart.Data<>(label, step.getModelInfo().getInputTokens()));
                outSeries.getData().add(new XYChart.Data<>(label, step.getModelInfo().getOutputTokens()));
            }
        }

        tokenLineChart.getData().addAll(inSeries, outSeries);
    }

    private void updateDurationBarChart(List<StepData> steps) {
        durationBarChart.getData().clear();

        Map<String, List<Double>> toolDurations = new LinkedHashMap<>();
        for (StepData step : steps) {
            if (step.getToolCalls() == null) continue;
            for (ToolCallData tc : step.getToolCalls()) {
                if (tc.getDurationMs() != null) {
                    toolDurations.computeIfAbsent(tc.getToolName(), k -> new ArrayList<>())
                            .add(tc.getDurationMs());
                }
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, List<Double>> entry : toolDurations.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(v -> v).average().orElse(0);
            series.getData().add(new XYChart.Data<>(entry.getKey(), Math.round(avg)));
        }
        durationBarChart.getData().add(series);
    }

    private void showEmpty(boolean empty) {
        emptyText.setVisible(empty);
        emptyText.setManaged(empty);
        tokenLineChart.setVisible(!empty);
        tokenLineChart.setManaged(!empty);
        durationBarChart.setVisible(!empty);
        durationBarChart.setManaged(!empty);
    }
}
