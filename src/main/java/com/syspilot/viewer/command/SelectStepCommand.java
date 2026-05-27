package com.syspilot.viewer.command;

import com.syspilot.viewer.architecture.BaseCommand;
import com.syspilot.viewer.event.StepSelectedEvent;
import com.syspilot.viewer.model.StepData;

public class SelectStepCommand extends BaseCommand {

    private final StepData step;

    public SelectStepCommand(StepData step) {
        this.step = step;
    }

    @Override
    public void execute() {
        sendEvent(new StepSelectedEvent(step));
    }
}
