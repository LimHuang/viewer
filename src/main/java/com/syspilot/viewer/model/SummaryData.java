package com.syspilot.viewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SummaryData {
    @JsonProperty("total_steps")
    private int totalSteps;

    @JsonProperty("main_steps")
    private int mainSteps;

    @JsonProperty("subagent_count")
    private int subagentCount;

    @JsonProperty("total_tokens_in")
    private int totalTokensIn;

    @JsonProperty("total_tokens_out")
    private int totalTokensOut;

    @JsonProperty("tool_usage")
    private Map<String, Integer> toolUsage;

    private List<String> errors;

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public int getMainSteps() { return mainSteps; }
    public void setMainSteps(int mainSteps) { this.mainSteps = mainSteps; }

    public int getSubagentCount() { return subagentCount; }
    public void setSubagentCount(int subagentCount) { this.subagentCount = subagentCount; }

    public int getTotalTokensIn() { return totalTokensIn; }
    public void setTotalTokensIn(int totalTokensIn) { this.totalTokensIn = totalTokensIn; }

    public int getTotalTokensOut() { return totalTokensOut; }
    public void setTotalTokensOut(int totalTokensOut) { this.totalTokensOut = totalTokensOut; }

    public Map<String, Integer> getToolUsage() { return toolUsage; }
    public void setToolUsage(Map<String, Integer> toolUsage) { this.toolUsage = toolUsage; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getToolUsageText() {
        if (toolUsage == null || toolUsage.isEmpty()) return "No tool calls";
        StringBuilder sb = new StringBuilder();
        toolUsage.forEach((name, count) -> sb.append(name).append(": ").append(count).append(", "));
        return sb.substring(0, sb.length() - 2);
    }
}
