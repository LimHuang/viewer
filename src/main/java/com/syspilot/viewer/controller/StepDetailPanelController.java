package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.event.StepSelectedEvent;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.StepData;
import com.syspilot.viewer.model.ToolCallData;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class StepDetailPanelController extends BaseController {

    @FXML private HBox stepHeader;
    @FXML private Text stepTitle;
    @FXML private Label roleBadge;
    @FXML private Text stepTimeText;
    @FXML private Text stepDurationText;

    @FXML private HBox statsRow;
    @FXML private Text tokensInDetail;
    @FXML private Text tokensOutDetail;
    @FXML private Text llmTimeText;
    @FXML private Text stepTimeDetail;

    @FXML private Separator headerSeparator;

    @FXML private VBox reasoningSection;
    @FXML private TextArea reasoningArea;

    @FXML private VBox messageSection;
    @FXML private TextArea messageArea;

    @FXML private VBox resultSection;
    @FXML private Text resultText;

    @FXML private VBox toolSection;
    @FXML private ListView<ToolCallData> toolListView;

    @FXML private VBox emptyState;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());

        toolListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ToolCallData tc, boolean empty) {
                super.updateItem(tc, empty);
                if (empty || tc == null) {
                    setText(null);
                } else {
                    String icon = tc.hasError() ? "✗" : "✓";
                    String time = tc.getDurationMs() != null
                            ? String.format(" (%.0fms)", tc.getDurationMs()) : "";
                    setText(icon + " " + tc.getToolName() + time);
                }
            }
        });

        registerEvent(StepSelectedEvent.class, this::onStepSelected);
        registerEvent(TrajectoryLoadedEvent.class, e -> clear());
    }

    private void onStepSelected(StepSelectedEvent event) {
        showDetail(true);
        StepData step = event.getStep();

        // Header
        stepTitle.setText("Step #" + step.getStepId());

        roleBadge.setText(getRoleLabel(step));
        roleBadge.getStyleClass().removeAll("role-badge-agent", "role-badge-subagent", "role-badge-user");
        switch (getRoleType(step)) {
            case "agent" -> roleBadge.getStyleClass().add("role-badge-agent");
            case "subagent" -> roleBadge.getStyleClass().add("role-badge-subagent");
            default -> roleBadge.getStyleClass().add("role-badge-user");
        }

        stepTimeText.setText(step.getTimestamp() != null ? step.getTimestamp() : "");
        stepDurationText.setText(step.getStepDurationMs() != null
                ? String.format("%.1fs", step.getStepDurationMs() / 1000) : "");

        // Stats row
        if (step.getModelInfo() != null) {
            tokensInDetail.setText(formatNum(step.getModelInfo().getInputTokens()));
            tokensOutDetail.setText(formatNum(step.getModelInfo().getOutputTokens()));
            statsRow.setVisible(true);
            statsRow.setManaged(true);
        } else {
            statsRow.setVisible(false);
            statsRow.setManaged(false);
        }

        double stepTime = step.getStepDurationMs() != null ? step.getStepDurationMs() / 1000 : 0;
        stepTimeDetail.setText(String.format("%.1fs", stepTime));
        llmTimeText.setText(String.format("%.1fs", stepTime));

        // Reasoning
        if (step.getReasoning() != null && !step.getReasoning().isEmpty()) {
            reasoningArea.setText(step.getReasoning());
            reasoningSection.setVisible(true);
            reasoningSection.setManaged(true);
        } else {
            reasoningSection.setVisible(false);
            reasoningSection.setManaged(false);
        }

        // Message
        if (step.getMessage() != null && !step.getMessage().isEmpty()) {
            messageArea.setText(step.getMessage());
            messageSection.setVisible(true);
            messageSection.setManaged(true);
        } else {
            messageSection.setVisible(false);
            messageSection.setManaged(false);
        }

        // Result box (show for final steps with success)
        boolean isComplete = step.getMessage() != null &&
                (step.getMessage().contains("completed") || step.getMessage().contains("success"));
        if (isComplete) {
            resultText.setText(step.getMessage());
            resultSection.setVisible(true);
            resultSection.setManaged(true);
        } else {
            resultSection.setVisible(false);
            resultSection.setManaged(false);
        }

        // Tool calls
        toolListView.getItems().clear();
        if (step.getToolCalls() != null && !step.getToolCalls().isEmpty()) {
            toolListView.getItems().addAll(step.getToolCalls());
            toolSection.setVisible(true);
            toolSection.setManaged(true);
        } else {
            toolSection.setVisible(false);
            toolSection.setManaged(false);
        }

        headerSeparator.setVisible(true);
        headerSeparator.setManaged(true);
    }

    private void clear() {
        showDetail(false);
        stepTitle.setText("");
        roleBadge.setText("");
        stepTimeText.setText("");
        stepDurationText.setText("");
        reasoningArea.clear();
        messageArea.clear();
        toolListView.getItems().clear();
        headerSeparator.setVisible(false);
        headerSeparator.setManaged(false);
    }

    private void showDetail(boolean show) {
        stepHeader.setVisible(show);
        stepHeader.setManaged(show);
        emptyState.setVisible(!show);
        emptyState.setManaged(!show);
    }

    private String getRoleLabel(StepData s) {
        if (s.getToolCalls() != null && s.getToolCalls().stream().anyMatch(tc -> tc.getSubagent() != null)) {
            return "SUBAGENT";
        }
        return "agent".equals(s.getType()) ? "AGENT" : "USER";
    }

    private String getRoleType(StepData s) {
        if (s.getToolCalls() != null && s.getToolCalls().stream().anyMatch(tc -> tc.getSubagent() != null)) {
            return "subagent";
        }
        return s.getType();
    }

    private static String formatNum(int n) {
        if (n >= 1000000) return String.format("%,d", n);
        if (n >= 1000) return String.format("%,d", n);
        return String.valueOf(n);
    }
}
