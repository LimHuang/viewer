package com.syspilot.viewer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class StepData {
    @JsonProperty("step_id")
    private int stepId;

    private String type;
    private String timestamp;
    private String message = "";
    private String reasoning = "";

    @JsonProperty("model_info")
    private ModelInfoData modelInfo;

    @JsonProperty("tool_calls")
    private List<ToolCallData> toolCalls;

    @JsonProperty("step_duration_ms")
    private Double stepDurationMs;

    public int getStepId() { return stepId; }
    public void setStepId(int stepId) { this.stepId = stepId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public ModelInfoData getModelInfo() { return modelInfo; }
    public void setModelInfo(ModelInfoData modelInfo) { this.modelInfo = modelInfo; }

    public List<ToolCallData> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallData> toolCalls) { this.toolCalls = toolCalls; }

    public Double getStepDurationMs() { return stepDurationMs; }
    public void setStepDurationMs(Double stepDurationMs) { this.stepDurationMs = stepDurationMs; }

    public String getDisplayText() {
        if ("user".equals(type)) {
            String msg = message != null ? message : "";
            if (msg.length() > 50) msg = msg.substring(0, 50) + "...";
            return msg;
        }
        int toolCount = toolCalls != null ? toolCalls.size() : 0;
        return "Agent" + (toolCount > 0 ? " (" + toolCount + " tools)" : "");
    }

    public String getDurationText() {
        if (stepDurationMs == null) return "";
        return String.format("%.1fs", stepDurationMs / 1000);
    }
}
