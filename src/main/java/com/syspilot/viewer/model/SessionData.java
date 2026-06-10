package com.syspilot.viewer.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SessionData {
    private final Path dir;
    private final String name;
    private final List<TurnData> turns = new ArrayList<>();

    public SessionData(Path dir, String name) {
        this.dir = dir;
        this.name = name;
    }

    public Path getDir() { return dir; }
    public String getName() { return name; }
    public List<TurnData> getTurns() { return turns; }

    public void addTurn(TurnData turn) { turns.add(turn); }

    @Override
    public String toString() { return name + " (" + turns.size() + " turns)"; }
}
