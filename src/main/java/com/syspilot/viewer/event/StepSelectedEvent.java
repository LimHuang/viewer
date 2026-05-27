package com.syspilot.viewer.event;

import com.syspilot.viewer.model.StepData;

public class StepSelectedEvent {
    private final StepData step;

    public StepSelectedEvent(StepData step) {
        this.step = step;
    }

    public StepData getStep() { return step; }
}
