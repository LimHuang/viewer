package com.syspilot.viewer.architecture;

public abstract class BaseCommand {

    private AppArchitecture arch;

    public void setArchitecture(AppArchitecture arch) {
        this.arch = arch;
    }

    protected AppArchitecture getArch() { return arch; }

    protected <T> T getSystem(Class<T> type) {
        return arch.getSystem(type);
    }

    protected <T> T getModel(Class<T> type) {
        return arch.getModel(type);
    }

    protected <T> T getUtility(Class<T> type) {
        return arch.getUtility(type);
    }

    protected <T> void sendEvent(T event) {
        arch.sendEvent(event);
    }

    protected void sendCommand(BaseCommand command) {
        arch.sendCommand(command);
    }

    public abstract void execute();
}
