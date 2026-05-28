package com.syspilot.viewer.command;

import com.syspilot.viewer.architecture.BaseCommand;
import com.syspilot.viewer.model.TrajectoryData;
import com.syspilot.viewer.system.TrajectorySystem;
import com.syspilot.viewer.utility.TrajectoryLoader;
import java.io.File;
import java.io.IOException;

public class LoadTrajectoryCommand extends BaseCommand {

    private final File file;

    public LoadTrajectoryCommand(File file) {
        this.file = file;
    }

    @Override
    public void execute() {
        TrajectoryLoader loader = getUtility(TrajectoryLoader.class);
        TrajectorySystem system = getSystem(TrajectorySystem.class);
        try {
            TrajectoryData data = loader.load(file);
            system.addTrajectory(file.getAbsolutePath(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load trajectory: " + e.getMessage(), e);
        }
    }
}
