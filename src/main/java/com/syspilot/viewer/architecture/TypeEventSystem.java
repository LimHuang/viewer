package com.syspilot.viewer.architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TypeEventSystem {

    private final Map<Class<?>, List<Consumer<?>>> events = new HashMap<>();

    public <T> void send(T event) {
        @SuppressWarnings("unchecked")
        List<Consumer<T>> listeners = (List<Consumer<T>>) (Object) events.get(event.getClass());
        if (listeners != null) {
            for (Consumer<T> listener : listeners) {
                listener.accept(event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Disposable register(Class<T> eventType, Consumer<T> listener) {
        events.computeIfAbsent(eventType, k -> new ArrayList<>())
              .add(listener);
        return () -> {
            List<Consumer<?>> list = events.get(eventType);
            if (list != null) {
                list.remove(listener);
            }
        };
    }

    public void clear() {
        events.clear();
    }
}
