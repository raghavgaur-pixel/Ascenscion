package com.ascension.assets.registry;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.RegistryDescriptor;
import com.ascension.registry.ReloadableRegistry;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Asset registry view backed by the generic registry framework.
 *
 * @param <T> asset type
 */
public final class RegistryBackedAssetRegistry<T extends AssetDefinition> implements AssetRegistry<T> {

    private final ReloadableRegistry<AssetId, T> registry;

    public RegistryBackedAssetRegistry(final ReloadableRegistry<AssetId, T> registry) {
        this.registry = registry;
    }

    @Override
    public RegistryDescriptor<AssetId, T> descriptor() {
        return this.registry.descriptor();
    }

    @Override
    public java.util.Optional<T> find(final AssetId key) {
        return this.registry.find(key);
    }

    @Override
    public T require(final AssetId key) {
        return this.registry.require(key);
    }

    @Override
    public Collection<T> values() {
        return this.registry.values();
    }

    @Override
    public Collection<AssetId> keys() {
        return this.registry.keys();
    }

    @Override
    public boolean contains(final AssetId key) {
        return this.registry.contains(key);
    }

    @Override
    public Collection<T> namespace(final String namespace) {
        return this.registry.values().stream()
            .filter(asset -> asset.id().namespace().equals(namespace))
            .collect(Collectors.toList());
    }
}

