package com.syspilot.viewer.architecture;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseController {

    private AppArchitecture arch;
    private final List<Disposable> disposables = new ArrayList<>();

    public void setArchitecture(AppArchitecture arch) {
        this.arch = arch;
    }

    protected AppArchitecture getArch() {
        return arch;
    }

    protected <T> T getSystem(Class<T> type) {
        return arch.getSystem(type);
    }

    protected <T> T getModel(Class<T> type) {
        return arch.getModel(type);
    }

    protected <T> T getUtility(Class<T> type) {
        return arch.getUtility(type);
    }

    protected void sendCommand(BaseCommand command) {
        arch.sendCommand(command);
    }

    protected <T> void sendEvent(T event) {
        arch.sendEvent(event);
    }

    protected <T> void registerEvent(Class<T> eventType, java.util.function.Consumer<T> listener) {
        disposables.add(arch.registerEvent(eventType, listener));
    }

    protected void addDisposable(Disposable d) {
        disposables.add(d);
    }

    public void dispose() {
        for (Disposable d : disposables) {
            d.dispose();
        }
        disposables.clear();
    }
}
