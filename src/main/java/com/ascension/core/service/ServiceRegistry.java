package com.ascension.core.service;

import com.ascension.core.di.ServiceContainer;
import java.util.Objects;

public final class ServiceRegistry {

    private final ServiceContainer container;

    public ServiceRegistry(final ServiceContainer container) {
        this.container = Objects.requireNonNull(container, "container");
    }

    public <T> void register(final Class<T> type, final T instance) {
        this.container.registerInstance(type, instance);
    }

    public <T> T require(final Class<T> type) {
        return this.container.resolve(type);
    }

    public boolean has(final Class<?> type) {
        return this.container.contains(type);
    }
}

