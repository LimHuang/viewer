package com.syspilot.viewer.event;

public class NavigationEvent {
    private final String targetTab;

    public NavigationEvent(String targetTab) {
        this.targetTab = targetTab;
    }

    public String getTargetTab() { return targetTab; }
}
