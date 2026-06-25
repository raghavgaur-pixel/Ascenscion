package com.ascension.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry implementation.
 *
 * @param <K> registry key type
 * @param <V> registry value type
 */
public final class ConcurrentMutableRegistry<K, V> implements ReloadableRegistry<K, V> {

    private final RegistryDescriptor<K, V> descriptor;
    private final Map<K, V> values = new ConcurrentHashMap<>();

    public ConcurrentMutableRegistry(final RegistryDescriptor<K, V> descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public RegistryDescriptor<K, V> descriptor() {
        return this.descriptor;
    }

    @Override
    public Optional<V> find(final K key) {
        return Optional.ofNullable(this.values.get(key));
    }

    @Override
    public V require(final K key) {
        return this.find(key)
            .orElseThrow(() -> new IllegalStateException(
                "Missing registry entry '" + key + "' in registry '" + this.descriptor.name() + "'."
            ));
    }

    @Override
    public Collection<V> values() {
        return java.util.List.copyOf(this.values.values());
    }

    @Override
    public Collection<K> keys() {
        return java.util.List.copyOf(this.values.keySet());
    }

    @Override
    public boolean contains(final K key) {
        return this.values.containsKey(key);
    }

    @Override
    public void register(final K key, final V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        final V previous = this.values.putIfAbsent(key, value);
        if (previous != null) {
            throw new IllegalStateException(
                "Duplicate registry entry '" + key + "' in registry '" + this.descriptor.name() + "'."
            );
        }
    }

    @Override
    public void replaceAll(final Map<K, V> values) {
        Objects.requireNonNull(values, "values");
        this.values.clear();
        for (final Map.Entry<K, V> entry : values.entrySet()) {
            this.values.put(
                Objects.requireNonNull(entry.getKey(), "key"),
                Objects.requireNonNull(entry.getValue(), "value")
            );
        }
    }

    @Override
    public void clear() {
        this.values.clear();
    }
}
