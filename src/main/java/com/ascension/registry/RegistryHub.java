package com.ascension.registry;

import java.util.Collection;

/**
 * Service that owns every top-level registry in the server runtime.
 */
public interface RegistryHub {

    /**
     * Creates a registry if it does not yet exist, otherwise returns the existing registry.
     *
     * @param descriptor registry descriptor
     * @param <K> registry key type
     * @param <V> registry value type
     * @return mutable registry instance
     */
    <K, V> MutableRegistry<K, V> getOrCreate(RegistryDescriptor<K, V> descriptor);

    /**
     * Returns an existing registry.
     *
     * @param descriptor registry descriptor
     * @param <K> registry key type
     * @param <V> registry value type
     * @return registry instance
     */
    <K, V> Registry<K, V> require(RegistryDescriptor<K, V> descriptor);

    /**
     * @return all registry descriptors known to the hub
     */
    Collection<RegistryDescriptor<?, ?>> descriptors();
}

