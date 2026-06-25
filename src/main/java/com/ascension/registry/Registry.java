package com.ascension.registry;

import java.util.Collection;
import java.util.Optional;

/**
 * Read-only lookup contract for a runtime registry.
 *
 * @param <K> registry key type
 * @param <V> registry value type
 */
public interface Registry<K, V> {

    /**
     * @return stable registry descriptor
     */
    RegistryDescriptor<K, V> descriptor();

    /**
     * Resolves a value by key.
     *
     * @param key registry key
     * @return registered value if present
     */
    Optional<V> find(K key);

    /**
     * Resolves a value by key and fails if missing.
     *
     * @param key registry key
     * @return registered value
     */
    V require(K key);

    /**
     * @return immutable snapshot of registered values
     */
    Collection<V> values();

    /**
     * @return immutable snapshot of registered keys
     */
    Collection<K> keys();

    /**
     * Checks whether a key is registered.
     *
     * @param key registry key
     * @return {@code true} if registered
     */
    boolean contains(K key);
}

