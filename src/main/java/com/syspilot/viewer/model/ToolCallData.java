package com.syspilot.viewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCallData {
    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_name")
    private String toolName;

    private Map<String, Object> args;
    private String result;
    private String error;

    @JsonProperty("duration_ms")
    private Double durationMs;

    private SubAgentData subagent;

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getArgs() { return args; }
    public void setArgs(Map<String, Object> args) { this.args = args; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Double getDurationMs() { return durationMs; }
    public void setDurationMs(Double durationMs) { this.durationMs = durationMs; }

    public SubAgentData getSubagent() { return subagent; }
    public void setSubagent(SubAgentData subagent) { this.subagent = subagent; }

    public boolean hasError() { return error != null && !error.isEmpty(); }

    public String getDurationText() {
        if (durationMs == null) return "";
        return String.format("%.0fms", durationMs);
    }
}
