package com.ascension.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central concurrent registry hub implementation.
 */
public final class DefaultRegistryHub implements RegistryHub {

    private final Map<String, MutableRegistry<?, ?>> registries = new ConcurrentHashMap<>();
    private final Map<String, RegistryDescriptor<?, ?>> descriptors = new ConcurrentHashMap<>();

    @Override
    public <K, V> MutableRegistry<K, V> getOrCreate(final RegistryDescriptor<K, V> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");

        this.descriptors.putIfAbsent(descriptor.name(), descriptor);
        final MutableRegistry<?, ?> registry = this.registries.computeIfAbsent(
            descriptor.name(),
            ignored -> new ConcurrentMutableRegistry<>(descriptor)
        );
        return castRegistry(descriptor, registry);
    }

    @Override
    public <K, V> Registry<K, V> require(final RegistryDescriptor<K, V> descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");

        final MutableRegistry<?, ?> registry = this.registries.get(descriptor.name());
        if (registry == null) {
            throw new IllegalStateException("Registry not initialized: " + descriptor.name());
        }
        return castRegistry(descriptor, registry);
    }

    @Override
    public Collection<RegistryDescriptor<?, ?>> descriptors() {
        return java.util.List.copyOf(this.descriptors.values());
    }

    @SuppressWarnings("unchecked")
    private static <K, V> MutableRegistry<K, V> castRegistry(
        final RegistryDescriptor<K, V> descriptor,
        final MutableRegistry<?, ?> registry
    ) {
        final RegistryDescriptor<?, ?> existingDescriptor = registry.descriptor();
        if (!existingDescriptor.keyType().equals(descriptor.keyType())
            || !existingDescriptor.valueType().equals(descriptor.valueType())) {
            throw new IllegalStateException(
                "Registry descriptor mismatch for '" + descriptor.name() + "'."
            );
        }
        return (MutableRegistry<K, V>) registry;
    }
}

