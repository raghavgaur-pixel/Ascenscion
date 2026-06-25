package com.ascension.registry;

/**
 * Mutable registry contract for runtime registration.
 *
 * @param <K> registry key type
 * @param <V> registry value type
 */
public interface MutableRegistry<K, V> extends Registry<K, V> {

    /**
     * Registers a new value.
     *
     * @param key registry key
     * @param value registry value
     */
    void register(K key, V value);
}

