package com.syspilot.viewer.system;

import com.syspilot.viewer.architecture.BaseSystem;
import com.syspilot.viewer.event.TrajectoryLoadedEvent;
import com.syspilot.viewer.model.TrajectoryData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.*;

public class TrajectorySystem extends BaseSystem {

    private final ObjectProperty<TrajectoryData> trajectory = new SimpleObjectProperty<>();
    private final Map<String, TrajectoryData> trajectories = new LinkedHashMap<>();
    private String activeKey;

    public ObjectProperty<TrajectoryData> trajectoryProperty() { return trajectory; }
    public TrajectoryData getTrajectory() { return trajectory.get(); }
    public String getActiveKey() { return activeKey; }
    public Set<String> getOpenFiles() { return Collections.unmodifiableSet(trajectories.keySet()); }
    public boolean isEmpty() { return trajectories.isEmpty(); }

    public void setTrajectory(TrajectoryData data) {
        String key = data.getTaskId() != null ? data.getTaskId() : UUID.randomUUID().toString();
        addTrajectory(key, data);
    }

    public void addTrajectory(String key, TrajectoryData data) {
        trajectories.put(key, data);
        switchTo(key);
    }

    public void switchTo(String key) {
        activeKey = key;
        TrajectoryData data = trajectories.get(key);
        if (data != null) {
            trajectory.set(data);
            sendEvent(new TrajectoryLoadedEvent(data));
        }
    }

    public void removeTrajectory(String key) {
        trajectories.remove(key);
        if (key.equals(activeKey)) {
            String next = !trajectories.isEmpty()
                    ? trajectories.keySet().iterator().next() : null;
            if (next != null) {
                switchTo(next);
            } else {
                activeKey = null;
                trajectory.set(null);
            }
        }
    }

    @Override
    protected void onInit() {}

    @Override
    protected void onDeinit() {
        trajectory.set(null);
        trajectories.clear();
        activeKey = null;
    }
}
