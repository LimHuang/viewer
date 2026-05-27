package com.syspilot.viewer.system;

import com.syspilot.viewer.architecture.BaseSystem;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.TrajectoryData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class TrajectorySystem extends BaseSystem {

    private final ObjectProperty<TrajectoryData> trajectory = new SimpleObjectProperty<>();

    public ObjectProperty<TrajectoryData> trajectoryProperty() { return trajectory; }
    public TrajectoryData getTrajectory() { return trajectory.get(); }

    public void setTrajectory(TrajectoryData data) {
        trajectory.set(data);
        sendEvent(new TrajectoryLoadedEvent(data));
    }

    @Override
    protected void onInit() {}

    @Override
    protected void onDeinit() {
        trajectory.set(null);
    }
}
