package com.ascension.state;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe arbitrary runtime metadata store.
 */
public final class TemporaryMetadata {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    /**
     * Stores a metadata value.
     *
     * @param key metadata key
     * @param value metadata value
     */
    public void put(final String key, final Object value) {
        this.values.put(key, value);
    }

    /**
     * Reads a typed metadata value.
     *
     * @param key metadata key
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
     * Removes a metadata value.
     *
     * @param key metadata key
     */
    public void remove(final String key) {
        this.values.remove(key);
    }

    /**
     * Clears every metadata value.
     */
    public void clear() {
        this.values.clear();
    }
}

