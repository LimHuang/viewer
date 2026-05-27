package com.syspilot.viewer.model;

import javafx.scene.control.TreeItem;

public class TreeNodeData {

    public enum NodeType {
        AGENT_ROOT,    // Main agent or subagent root
        STEP,          // A step within an agent
        TOOL_CALL,     // A tool call that spawned a subagent
        SUBAGENT       // Subagent node
    }

    private final NodeType type;
    private final String label;
    private final TreeItem<TreeNodeData> treeItem;

    // For STEP nodes
    private StepData step;

    // For TOOL_CALL nodes
    private ToolCallData toolCall;

    // For AGENT_ROOT / SUBAGENT nodes
    private String agentId;
    private String agentLabel;
    private String agentStatus;

    // ---- Factory methods ----

    public static TreeNodeData createAgentRoot(String agentId, String label, String status) {
        TreeNodeData d = new TreeNodeData(NodeType.AGENT_ROOT, "Agent: " + label);
        d.agentId = agentId;
        d.agentLabel = label;
        d.agentStatus = status;
        return d;
    }

    public static TreeNodeData createSubAgent(SubAgentData sub) {
        String label = sub.getLabel() != null ? sub.getLabel() : sub.getAgentId();
        TreeNodeData d = new TreeNodeData(NodeType.SUBAGENT, label);
        d.agentId = sub.getAgentId();
        d.agentLabel = label;
        d.agentStatus = sub.getStatus();
        return d;
    }

    public static TreeNodeData createStep(StepData step) {
        String prefix = "#" + step.getStepId();
        TreeNodeData d = new TreeNodeData(NodeType.STEP, prefix);
        d.step = step;
        return d;
    }

    public static TreeNodeData createToolCall(ToolCallData tc) {
        String label = "spawned: " + tc.getToolName();
        TreeNodeData d = new TreeNodeData(NodeType.TOOL_CALL, label);
        d.toolCall = tc;
        return d;
    }

    private TreeNodeData(NodeType type, String label) {
        this.type = type;
        this.label = label;
        this.treeItem = new TreeItem<>(this);
    }

    // ---- Getters ----

    public NodeType getType() { return type; }
    public String getLabel() { return label; }
    public TreeItem<TreeNodeData> getTreeItem() { return treeItem; }

    public StepData getStep() { return step; }
    public ToolCallData getToolCall() { return toolCall; }
    public String getAgentId() { return agentId; }
    public String getAgentLabel() { return agentLabel; }
    public String getAgentStatus() { return agentStatus; }

    /**
     * Returns the first tool name from this step's tool calls, or message preview for user steps.
     */
    public String getActionDisplay() {
        if (type == NodeType.STEP && step != null) {
            if ("user".equals(step.getType())) {
                String msg = step.getMessage();
                if (msg != null && !msg.isEmpty()) {
                    return msg.length() > 50 ? msg.substring(0, 50) + "..." : msg;
                }
                return "(user message)";
            }
            if (step.getToolCalls() != null && !step.getToolCalls().isEmpty()) {
                return step.getToolCalls().get(0).getToolName();
            }
            return "(agent)";
        }
        if (type == NodeType.TOOL_CALL && toolCall != null) {
            String time = toolCall.getDurationMs() != null
                    ? String.format(" (%.0fms)", toolCall.getDurationMs()) : "";
            return "Agent → " + (toolCall.getSubagent() != null
                    ? toolCall.getSubagent().getLabel() : "?") + time;
        }
        return label;
    }

    public String getDurationText() {
        if (type == NodeType.STEP && step != null) {
            return step.getStepDurationMs() != null
                    ? String.format("%.1fs", step.getStepDurationMs() / 1000) : "";
        }
        return "";
    }

    public String getTokenInText() {
        if (type == NodeType.STEP && step != null && step.getModelInfo() != null) {
            return formatTokens(step.getModelInfo().getInputTokens());
        }
        return "";
    }

    public String getTokenOutText() {
        if (type == NodeType.STEP && step != null && step.getModelInfo() != null) {
            return formatTokens(step.getModelInfo().getOutputTokens());
        }
        return "";
    }

    public String getStatusText() {
        if ((type == NodeType.AGENT_ROOT || type == NodeType.SUBAGENT) && agentStatus != null) {
            return "[" + agentStatus + "]";
        }
        return "";
    }

    private static String formatTokens(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000.0);
        if (n >= 1000) return String.format("%.1fk", n / 1000.0);
        return String.valueOf(n);
    }
}
