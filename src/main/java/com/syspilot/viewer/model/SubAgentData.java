package com.syspilot.viewer.model;

import java.util.List;

public class SubAgentData {
    private String agentId;
    private String label;
    private String parentAgentId;
    private String status;
    private List<StepData> steps;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getParentAgentId() { return parentAgentId; }
    public void setParentAgentId(String parentAgentId) { this.parentAgentId = parentAgentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<StepData> getSteps() { return steps; }
    public void setSteps(List<StepData> steps) { this.steps = steps; }
}
