package com.ascension.core.cache;

import java.util.Collection;
import java.util.Optional;

/**
 * Thread-safe cache abstraction used by persistence and runtime services.
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
public interface Cache<K, V> {

    /**
     * Reads a value from the cache.
     *
     * @param key cache key
     * @return cached value if present
     */
    Optional<V> get(K key);

    /**
     * Stores or replaces a cached value.
     *
     * @param key cache key
     * @param value cached value
     */
    void put(K key, V value);

    /**
     * Removes a cached value.
     *
     * @param key cache key
     * @return removed value if present
     */
    Optional<V> remove(K key);

    /**
     * @return snapshot of cached values
     */
    Collection<V> values();

    /**
     * Clears every cached entry.
     */
    void clear();
}

