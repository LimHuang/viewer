package com.syspilot.viewer.controller;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.architecture.BaseController;
import com.syspilot.viewer.command.SelectStepCommand;
import com.syspilot.viewer.event.StepSelectedEvent;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Predicate;

public class StepListPanelController extends BaseController {

    @FXML private TextField filterField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private TreeView<TreeNodeData> agentTreeView;
    @FXML private Text emptyText;

    private TrajectoryData currentTrajectory;
    private TreeItem<TreeNodeData> fullTreeRoot;

    @FXML
    public void initialize() {
        setArchitecture(AppArchitecture.getInstance());

        typeFilter.getItems().addAll("All", "Agent", "User", "Sub-Agent");
        typeFilter.setValue("All");
        typeFilter.setOnAction(e -> applyFilter());

        filterField.textProperty().addListener((obs, old, val) -> applyFilter());

        agentTreeView.setCellFactory(lv -> new AgentTreeCell());
        agentTreeView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null && item.getValue() != null && item.getValue().getStep() != null) {
                sendCommand(new SelectStepCommand(item.getValue().getStep()));
            }
        });

        // Expand root by default
        agentTreeView.setShowRoot(true);

        registerEvent(TrajectoryLoadedEvent.class, this::onTrajectoryLoaded);
        registerEvent(StepSelectedEvent.class, this::onStepSelected);
    }

    private void onTrajectoryLoaded(TrajectoryLoadedEvent event) {
        currentTrajectory = event.getTrajectory();
        fullTreeRoot = buildFullTree(currentTrajectory);
        applyFilter();
    }

    /**
     * Build the full agent hierarchy tree from trajectory data.
     */
    private TreeItem<TreeNodeData> buildFullTree(TrajectoryData t) {
        TreeNodeData rootData = TreeNodeData.createAgentRoot(
                t.getTaskId() != null ? t.getTaskId() : "main",
                "Main Agent",
                t.isSuccess() ? "completed" : "running"
        );
        TreeItem<TreeNodeData> root = rootData.getTreeItem();

        if (t.getSteps() != null) {
            for (StepData step : t.getSteps()) {
                TreeItem<TreeNodeData> stepNode = addStepNode(step);
                root.getChildren().add(stepNode);
                addSubAgentChildren(stepNode, step);
            }
        }

        root.setExpanded(true);
        return root;
    }

    /**
     * Add sub-agent branches recursively under a step node.
     */
    private void addSubAgentChildren(TreeItem<TreeNodeData> parentStepNode, StepData step) {
        if (step.getToolCalls() == null) return;

        for (ToolCallData tc : step.getToolCalls()) {
            if (tc.getSubagent() != null) {
                SubAgentData sub = tc.getSubagent();

                // Tool call node: "spawned: Agent"
                TreeNodeData tcData = TreeNodeData.createToolCall(tc);
                TreeItem<TreeNodeData> tcNode = tcData.getTreeItem();
                parentStepNode.getChildren().add(tcNode);

                // Sub-agent node
                TreeNodeData subData = TreeNodeData.createSubAgent(sub);
                TreeItem<TreeNodeData> subNode = subData.getTreeItem();
                tcNode.getChildren().add(subNode);

                // Sub-agent's steps (recursive)
                if (sub.getSteps() != null) {
                    for (StepData subStep : sub.getSteps()) {
                        TreeItem<TreeNodeData> subStepNode = addStepNode(subStep);
                        subNode.getChildren().add(subStepNode);
                        addSubAgentChildren(subStepNode, subStep);
                    }
                }

                tcNode.setExpanded(true);
                subNode.setExpanded(true);
            }
        }
    }

    /**
     * Create a step tree node from step data.
     */
    private TreeItem<TreeNodeData> addStepNode(StepData step) {
        TreeNodeData data = TreeNodeData.createStep(step);
        return data.getTreeItem();
    }

    /**
     * Apply filter: rebuild tree with only matching nodes (keeping ancestors).
     */
    private void applyFilter() {
        if (currentTrajectory == null) {
            showEmpty(true);
            return;
        }

        String type = typeFilter.getValue();
        String search = filterField.getText() == null ? "" : filterField.getText().toLowerCase().trim();

        if (search.isEmpty() && "All".equals(type)) {
            showEmpty(false);
            agentTreeView.setRoot(fullTreeRoot);
            return;
        }

        // Build filtered copy
        TreeItem<TreeNodeData> filteredRoot = buildFilteredTree(fullTreeRoot, type, search);
        if (filteredRoot == null || hasNoStepChildren(filteredRoot)) {
            showEmpty(true);
        } else {
            showEmpty(false);
            agentTreeView.setRoot(filteredRoot);
        }
    }

    /**
     * Recursively build a filtered copy of the tree.
     * Returns null if this subtree has no matches (so ancestor can prune it).
     */
    private TreeItem<TreeNodeData> buildFilteredTree(TreeItem<TreeNodeData> source,
                                                      String type, String search) {
        TreeNodeData srcData = source.getValue();
        if (srcData == null) return null;

        boolean selfMatches = matchesFilter(srcData, type, search);
        boolean isStep = srcData.getType() == TreeNodeData.NodeType.STEP;

        // Build filtered children
        TreeItem<TreeNodeData> copy = new TreeItem<>(srcData);
        boolean hasMatchingChild = false;

        for (TreeItem<TreeNodeData> child : source.getChildren()) {
            TreeItem<TreeNodeData> filteredChild = buildFilteredTree(child, type, search);
            if (filteredChild != null) {
                copy.getChildren().add(filteredChild);
                hasMatchingChild = true;
            }
        }

        // Keep node if: it matches, or it has matching descendants, or it's a structural node (agent/subagent/tool)
        boolean isStructural = srcData.getType() == TreeNodeData.NodeType.AGENT_ROOT
                || srcData.getType() == TreeNodeData.NodeType.SUBAGENT
                || srcData.getType() == TreeNodeData.NodeType.TOOL_CALL;

        if (selfMatches || hasMatchingChild || (isStructural && hasMatchingChild)) {
            copy.setExpanded(true);
            return copy;
        }

        return null;
    }

    private boolean matchesFilter(TreeNodeData data, String type, String search) {
        if (data.getType() != TreeNodeData.NodeType.STEP) return false;

        StepData step = data.getStep();
        if (step == null) return false;

        // Type filter
        if (!"All".equals(type)) {
            if ("User".equals(type) && !"user".equals(step.getType())) return false;
            if ("Agent".equals(type) && !"agent".equals(step.getType())) return false;
            if ("Sub-Agent".equals(type)) {
                // Sub-agent steps are those under a SUBAGENT node
                return false; // handled structurally
            }
        }

        // Text search
        if (!search.isEmpty()) {
            boolean matches = false;
            if (step.getMessage() != null && step.getMessage().toLowerCase().contains(search)) matches = true;
            if (step.getDisplayText() != null && step.getDisplayText().toLowerCase().contains(search)) matches = true;
            if (!matches && step.getToolCalls() != null) {
                for (ToolCallData tc : step.getToolCalls()) {
                    if (tc.getToolName() != null && tc.getToolName().toLowerCase().contains(search)) {
                        matches = true;
                        break;
                    }
                }
            }
            return matches;
        }

        return true;
    }

    private boolean hasNoStepChildren(TreeItem<TreeNodeData> node) {
        if (node.getValue() != null && node.getValue().getType() == TreeNodeData.NodeType.STEP) return false;
        for (TreeItem<TreeNodeData> child : node.getChildren()) {
            if (!hasNoStepChildren(child)) return false;
        }
        return true;
    }

    private void showEmpty(boolean empty) {
        emptyText.setVisible(empty);
        emptyText.setManaged(empty);
        agentTreeView.setVisible(!empty);
        agentTreeView.setManaged(!empty);
    }

    private void onStepSelected(StepSelectedEvent event) {
        // Find and select the step in the tree
        selectStepInTree(agentTreeView.getRoot(), event.getStep().getStepId());
    }

    private boolean selectStepInTree(TreeItem<TreeNodeData> node, int stepId) {
        if (node == null) return false;
        TreeNodeData data = node.getValue();
        if (data != null && data.getType() == TreeNodeData.NodeType.STEP
                && data.getStep() != null && data.getStep().getStepId() == stepId) {
            agentTreeView.getSelectionModel().select(node);
            // Expand ancestors
            TreeItem<TreeNodeData> parent = node.getParent();
            while (parent != null) {
                parent.setExpanded(true);
                parent = parent.getParent();
            }
            agentTreeView.scrollTo(agentTreeView.getRow(node));
            return true;
        }
        for (TreeItem<TreeNodeData> child : node.getChildren()) {
            if (selectStepInTree(child, stepId)) return true;
        }
        return false;
    }

    // ---- TreeCell rendering ----

    private static class AgentTreeCell extends TreeCell<TreeNodeData> {
        @Override
        protected void updateItem(TreeNodeData data, boolean empty) {
            super.updateItem(data, empty);
            setGraphic(null);
            setText(null);
            setStyle("");

            if (empty || data == null) return;

            switch (data.getType()) {
                case AGENT_ROOT -> setGraphic(buildAgentRootRow(data));
                case SUBAGENT -> setGraphic(buildSubAgentRow(data));
                case TOOL_CALL -> setGraphic(buildToolCallRow(data));
                case STEP -> setGraphic(buildStepRow(data));
            }
        }

        private HBox buildAgentRootRow(TreeNodeData data) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 6;");

            Label badge = new Label("AGENT");
            badge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 9px;"
                    + " -fx-padding: 1 6; -fx-font-weight: bold; -fx-background-radius: 2;");

            Text label = new Text(data.getAgentLabel());
            label.setStyle("-fx-fill: #e0e0f0; -fx-font-weight: bold; -fx-font-size: 12px;");

            String status = data.getStatusText();
            Text statusText = new Text(status);
            statusText.setStyle("-fx-fill: #7a7a95; -fx-font-size: 10px;");

            row.getChildren().addAll(badge, label, statusText);
            return row;
        }

        private HBox buildSubAgentRow(TreeNodeData data) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 3 6;");

            Label badge = new Label("SUB");
            badge.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 9px;"
                    + " -fx-padding: 1 5; -fx-font-weight: bold; -fx-background-radius: 2;");

            Text label = new Text(data.getAgentLabel());
            label.setStyle("-fx-fill: #c8b8e0; -fx-font-weight: bold; -fx-font-size: 11px;");

            String status = data.getStatusText();
            if (status != null && !status.isEmpty()) {
                Text statusText = new Text(status);
                statusText.setStyle("-fx-fill: #7a7a95; -fx-font-size: 10px;");
                row.getChildren().addAll(badge, label, statusText);
            } else {
                row.getChildren().addAll(badge, label);
            }

            return row;
        }

        private HBox buildToolCallRow(TreeNodeData data) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 2 6 2 20;");

            Text icon = new Text("→");
            icon.setStyle("-fx-fill: #5dade2; -fx-font-size: 11px;");

            Text label = new Text(data.getActionDisplay());
            label.setStyle("-fx-fill: #8888a0; -fx-font-size: 11px; -fx-font-style: italic;");

            row.getChildren().addAll(icon, label);
            return row;
        }

        private HBox buildStepRow(TreeNodeData data) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 6 4 10;");

            // Step number
            Text numText = new Text("#" + (data.getStep() != null ? data.getStep().getStepId() : "?"));
            numText.setStyle("-fx-fill: #5a5a78; -fx-font-size: 11px; -fx-font-weight: bold;");

            // Role badge
            String role = data.getStep() != null ? data.getStep().getType() : "";
            Label badge = new Label();
            if ("user".equals(role)) {
                badge.setText("USER");
                badge.setStyle("-fx-background-color: #1a3a5c; -fx-text-fill: #5dade2;");
            } else {
                badge.setText("AGENT");
                badge.setStyle("-fx-background-color: #1a5c2a; -fx-text-fill: #4cdf80;");
            }
            badge.setStyle(badge.getStyle() + " -fx-font-size: 8px; -fx-padding: 1 5;"
                    + " -fx-font-weight: bold; -fx-background-radius: 2;");

            // Action / first tool name
            Text actionText = new Text(data.getActionDisplay());
            actionText.setStyle("-fx-fill: #b0b0c8; -fx-font-size: 12px;");
            HBox.setHgrow(actionText, Priority.ALWAYS);

            row.getChildren().addAll(numText, badge, actionText);

            // Token info (right-aligned)
            String tokenIn = data.getTokenInText();
            String tokenOut = data.getTokenOutText();
            if (!tokenIn.isEmpty() || !tokenOut.isEmpty()) {
                Text tiText = new Text((!tokenIn.isEmpty() ? tokenIn + " ⬆" : "")
                        + (!tokenOut.isEmpty() ? "  " + tokenOut + " ⬇" : ""));
                tiText.setStyle("-fx-fill: #6a6a85; -fx-font-size: 10px;");
                row.getChildren().add(tiText);
            }

            // Duration
            String dur = data.getDurationText();
            if (!dur.isEmpty()) {
                Text durText = new Text(dur);
                durText.setStyle("-fx-fill: #e0b040; -fx-font-size: 10px;");
                row.getChildren().add(durText);
            }

            return row;
        }
    }
}
