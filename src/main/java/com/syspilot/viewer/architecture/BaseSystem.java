package com.syspilot.viewer.architecture;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSystem {

    private AppArchitecture arch;
    private boolean initialized = false;
    private final List<Disposable> disposables = new ArrayList<>();

    public void setArchitecture(AppArchitecture arch) {
        this.arch = arch;
    }

    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean v) { this.initialized = v; }

    protected AppArchitecture getArch() { return arch; }

    @SuppressWarnings("unchecked")
    protected <T> T getSystem(Class<T> type) {
        return (T) arch.getSystem(type);
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

    protected <T> void registerEvent(Class<T> eventType, java.util.function.Consumer<T> listener) {
        disposables.add(arch.registerEvent(eventType, listener));
    }

    public void init() {
        onInit();
    }

    public void deinit() {
        for (Disposable d : disposables) {
            d.dispose();
        }
        disposables.clear();
        onDeinit();
    }

    protected abstract void onInit();
    protected void onDeinit() {}
}
