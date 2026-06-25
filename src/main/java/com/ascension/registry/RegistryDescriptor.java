package com.ascension.registry;

import java.util.Objects;

/**
 * Typed descriptor for a named registry.
 *
 * @param name stable registry name
 * @param keyType registry key type
 * @param valueType registry value type
 * @param <K> registry key type
 * @param <V> registry value type
 */
public record RegistryDescriptor<K, V>(String name, Class<K> keyType, Class<V> valueType) {

    public RegistryDescriptor {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");
    }
}

