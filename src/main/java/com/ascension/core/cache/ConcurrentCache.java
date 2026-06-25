package com.ascension.core.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory concurrent cache implementation without eviction.
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
public final class ConcurrentCache<K, V> implements Cache<K, V> {

    private final Map<K, V> values = new ConcurrentHashMap<>();

    @Override
    public Optional<V> get(final K key) {
        return Optional.ofNullable(this.values.get(key));
    }

    @Override
    public void put(final K key, final V value) {
        this.values.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    @Override
    public Optional<V> remove(final K key) {
        return Optional.ofNullable(this.values.remove(key));
    }

    @Override
    public Collection<V> values() {
        return java.util.List.copyOf(this.values.values());
    }

    @Override
    public void clear() {
        this.values.clear();
    }
}

