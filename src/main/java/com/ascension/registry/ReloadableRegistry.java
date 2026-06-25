package com.ascension.registry;

import java.util.Map;

/**
 * Mutable registry that supports atomic replacement for hot reload workflows.
 *
 * @param <K> registry key type
 * @param <V> registry value type
 */
public interface ReloadableRegistry<K, V> extends MutableRegistry<K, V> {

    /**
     * Atomically replaces all entries.
     *
     * @param values replacement values
     */
    void replaceAll(Map<K, V> values);

    /**
     * Clears every entry.
     */
    void clear();
}

