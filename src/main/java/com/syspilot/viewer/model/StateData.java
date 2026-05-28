package com.syspilot.viewer.model;

import java.util.ArrayList;
import java.util.List;

public class StateData {
    private List<String> openFiles = new ArrayList<>();
    private String activeFile;

    public StateData() {}

    public List<String> getOpenFiles() { return openFiles; }
    public void setOpenFiles(List<String> openFiles) { this.openFiles = openFiles; }

    public String getActiveFile() { return activeFile; }
    public void setActiveFile(String activeFile) { this.activeFile = activeFile; }
}
