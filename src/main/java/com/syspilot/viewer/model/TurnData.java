package com.syspilot.viewer.model;

import java.nio.file.Path;

public class TurnData {
    private final Path dir;
    private final String name;
    private final Path trajectoryFile;
    private String preview;

    public TurnData(Path dir, String name, Path trajectoryFile) {
        this.dir = dir;
        this.name = name;
        this.trajectoryFile = trajectoryFile;
    }

    public Path getDir() { return dir; }
    public String getName() { return name; }
    public Path getTrajectoryFile() { return trajectoryFile; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    @Override
    public String toString() { return name; }
}
