package com.syspilot.viewer.architecture;

import java.util.HashMap;
import java.util.Map;

public class AppArchitecture {

    private static AppArchitecture instance;

    private final Map<Class<?>, Object> container = new HashMap<>();
    private final TypeEventSystem eventSystem = new TypeEventSystem();
    private boolean inited = false;

    public static AppArchitecture getInstance() {
        if (instance == null) {
            instance = new AppArchitecture();
        }
        return instance;
    }

    public void registerSystem(Object system) {
        if (system instanceof BaseSystem s) s.setArchitecture(this);
        container.put(system.getClass(), system);
    }

    public void registerModel(Object model) {
        if (model instanceof BaseModel m) m.setArchitecture(this);
        container.put(model.getClass(), model);
    }

    public void registerUtility(Object utility) {
        container.put(utility.getClass(), utility);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSystem(Class<T> type) {
        return (T) container.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getModel(Class<T> type) {
        return (T) container.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getUtility(Class<T> type) {
        return (T) container.get(type);
    }

    public void sendCommand(BaseCommand command) {
        command.setArchitecture(this);
        command.execute();
    }

    public <T> void sendEvent(T event) {
        eventSystem.send(event);
    }

    public <T> Disposable registerEvent(Class<T> eventType, java.util.function.Consumer<T> listener) {
        return eventSystem.register(eventType, listener);
    }

    public void init() {
        if (inited) return;
        for (Object obj : container.values()) {
            if (obj instanceof BaseSystem s && !s.isInitialized()) {
                s.init();
                s.setInitialized(true);
            }
            if (obj instanceof BaseModel m && !m.isInitialized()) {
                m.init();
                m.setInitialized(true);
            }
        }
        inited = true;
    }

    public void deinit() {
        for (Object obj : container.values()) {
            if (obj instanceof BaseModel m && m.isInitialized()) m.deinit();
            if (obj instanceof BaseSystem s && s.isInitialized()) s.deinit();
        }
        container.clear();
        eventSystem.clear();
        instance = null;
        inited = false;
    }
}
