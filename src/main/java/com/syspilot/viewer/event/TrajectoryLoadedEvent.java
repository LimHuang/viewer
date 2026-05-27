package com.syspilot.viewer.event;

import com.syspilot.viewer.model.TrajectoryData;

public class TrajectoryLoadedEvent {
    private final TrajectoryData trajectory;

    public TrajectoryLoadedEvent(TrajectoryData trajectory) {
        this.trajectory = trajectory;
    }

    public TrajectoryData getTrajectory() { return trajectory; }
}
