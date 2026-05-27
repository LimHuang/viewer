package com.syspilot.viewer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelInfoData {
    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("input_tokens")
    private int inputTokens;

    @JsonProperty("output_tokens")
    private int outputTokens;

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
}
