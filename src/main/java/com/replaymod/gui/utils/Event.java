package com.replaymod.gui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Event<T> {
    private T invoker;
    private Function<List<T>, T> invokerFactory;
    private List<T> listeners = new ArrayList<>();
    private Event(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        update();
    }

    public static <T> Event<T> create(Function<List<T>, T> invokerFactory) {
        return new Event<>(invokerFactory);
    }

    void register(T listener) {
        listeners.add(listener);
        update();
    }

    void unregister(T listener) {
        listeners.remove(listener);
        update();
    }

    private void update() {
        invoker = invokerFactory.apply(new ArrayList<>(listeners));
    }

    public T invoker() {
        return invoker;
    }
}
