package com.syspilot.viewer.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syspilot.viewer.model.TrajectoryData;
import java.io.File;
import java.io.IOException;

public class TrajectoryLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public TrajectoryData load(File file) throws IOException {
        return mapper.readValue(file, TrajectoryData.class);
    }

    public TrajectoryData fromString(String json) throws IOException {
        return mapper.readValue(json, TrajectoryData.class);
    }
}
