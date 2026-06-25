package com.ascension.state;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe named context variable container for transient runtime values.
 */
public final class ContextVariables {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    /**
     * Sets a named variable.
     *
     * @param key variable name
     * @param value variable value
     */
    public void put(final String key, final Object value) {
        this.values.put(key, value);
    }

    /**
     * Reads a typed variable.
     *
     * @param key variable name
     * @param type requested type
     * @param <T> requested type
     * @return typed value if present and compatible
     */
    public <T> Optional<T> get(final String key, final Class<T> type) {
        final Object value = this.values.get(key);
        if (value == null || !type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    /**
     * Removes a variable.
     *
     * @param key variable name
     */
    public void remove(final String key) {
        this.values.remove(key);
    }

    /**
     * @return immutable snapshot
     */
    public Map<String, Object> snapshot() {
        return Map.copyOf(this.values);
    }

    /**
     * Clears every variable.
     */
    public void clear() {
        this.values.clear();
    }
}
