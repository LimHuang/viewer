package com.syspilot.viewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubAgentData {
    @JsonProperty("agent_id")
    private String agentId;

    private String label;

    @JsonProperty("parent_agent_id")
    private String parentAgentId;

    @JsonProperty("subagent_type")
    private String subagentType;

    private String description;

    private String status;

    @JsonProperty("spawned_by_tool_call_id")
    private String spawnedByToolCallId;

    private List<StepData> steps;

    private SummaryData summary;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getParentAgentId() { return parentAgentId; }
    public void setParentAgentId(String parentAgentId) { this.parentAgentId = parentAgentId; }

    public String getSubagentType() { return subagentType; }
    public void setSubagentType(String subagentType) { this.subagentType = subagentType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSpawnedByToolCallId() { return spawnedByToolCallId; }
    public void setSpawnedByToolCallId(String spawnedByToolCallId) { this.spawnedByToolCallId = spawnedByToolCallId; }

    public List<StepData> getSteps() { return steps; }
    public void setSteps(List<StepData> steps) { this.steps = steps; }

    public SummaryData getSummary() { return summary; }
    public void setSummary(SummaryData summary) { this.summary = summary; }
}
