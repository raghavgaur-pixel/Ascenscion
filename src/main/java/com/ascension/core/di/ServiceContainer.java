package com.ascension.core.di;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceContainer {

    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    public <T> void registerInstance(final Class<T> type, final T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");

        final Object previous = this.instances.putIfAbsent(type, instance);
        if (previous != null) {
            throw new IllegalStateException("Service already registered for type: " + type.getName());
        }
    }

    public <T> T resolve(final Class<T> type) {
        Objects.requireNonNull(type, "type");
        final Object instance = this.instances.get(type);
        if (instance == null) {
            throw new IllegalStateException("No service registered for type: " + type.getName());
        }
        return type.cast(instance);
    }

    public boolean contains(final Class<?> type) {
        return this.instances.containsKey(type);
    }
}

