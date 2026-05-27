package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.StepData;
import com.syspilot.viewer.model.SubAgentData;
import com.syspilot.viewer.model.ToolCallData;
import com.syspilot.viewer.model.TrajectoryData;
import com.syspilot.viewer.system.TrajectorySystem;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;

public class SubAgentTreePanelController extends BaseController {

    @FXML private TreeView<String> agentTreeView;
    @FXML private Text emptyText;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());
        registerEvent(TrajectoryLoadedEvent.class, this::onTrajectoryLoaded);

        TrajectoryData existing = getSystem(TrajectorySystem.class).getTrajectory();
        if (existing != null) {
            onTrajectoryLoaded(new TrajectoryLoadedEvent(existing));
        } else {
            showEmpty(true);
        }
    }

    private void onTrajectoryLoaded(TrajectoryLoadedEvent event) {
        TreeItem<String> root = new TreeItem<>("Main Agent");
        boolean hasSubAgents = false;

        for (StepData step : event.getTrajectory().getSteps()) {
            if (step.getToolCalls() == null) continue;
            for (ToolCallData tc : step.getToolCalls()) {
                if (tc.getSubagent() != null) {
                    hasSubAgents = true;
                    SubAgentData sub = tc.getSubagent();
                    String label = (sub.getLabel() != null ? sub.getLabel() : sub.getAgentId())
                            + " [" + (sub.getStatus() != null ? sub.getStatus() : "?") + "]";
                    TreeItem<String> subNode = new TreeItem<>(label);

                    if (sub.getSteps() != null) {
                        for (StepData subStep : sub.getSteps()) {
                            String text = "Step " + subStep.getStepId()
                                    + " (" + subStep.getType() + ")"
                                    + (subStep.getStepDurationMs() != null
                                    ? " " + String.format("%.0fms", subStep.getStepDurationMs()) : "");
                            subNode.getChildren().add(new TreeItem<>(text));
                        }
                    }

                    if (tc.getToolName() != null) {
                        subNode.getChildren().add(0, new TreeItem<>("via: " + tc.getToolName()));
                    }

                    root.getChildren().add(subNode);
                }
            }
        }

        root.setExpanded(true);
        agentTreeView.setRoot(root);
        showEmpty(!hasSubAgents);
    }

    private void showEmpty(boolean empty) {
        emptyText.setVisible(empty);
        emptyText.setManaged(empty);
        agentTreeView.setVisible(!empty);
        agentTreeView.setManaged(!empty);
    }
}
