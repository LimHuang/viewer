package com.syspilot.viewer.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syspilot.viewer.model.TrajectoryData;
import java.io.File;
import java.io.IOException;

public class TrajectoryLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public TrajectoryData load(File file) throws IOException {
        TrajectoryData data = mapper.readValue(file, TrajectoryData.class);
        if (data.getSchemaVersion() == null && data.getSteps() == null) {
            throw new IOException("Not a trajectory file: " + file.getName());
        }
        return data;
    }

    public TrajectoryData fromString(String json) throws IOException {
        return mapper.readValue(json, TrajectoryData.class);
    }
}
