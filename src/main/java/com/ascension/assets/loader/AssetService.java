package com.ascension.assets.loader;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.registry.AssetRegistry;
import java.util.Collection;

/**
 * Central asset loading, registration, and reload service.
 */
public interface AssetService extends com.ascension.assets.reload.AssetReloadService {

    /**
     * Registers an asset type with the framework.
     *
     * @param assetType asset type descriptor
     * @param <T> asset definition type
     */
    <T extends AssetDefinition> void registerType(AssetType<T> assetType);

    /**
     * Resolves a typed asset registry.
     *
     * @param assetType asset type descriptor
     * @param <T> asset definition type
     * @return registry view
     */
    <T extends AssetDefinition> AssetRegistry<T> registry(AssetType<T> assetType);

    /**
     * Resolves a registered asset type.
     *
     * @param assetTypeId asset type id
     * @return registered asset type
     */
    AssetType<?> requireType(String assetTypeId);

    /**
     * @return immutable snapshot of registered asset types
     */
    Collection<AssetType<?>> assetTypes();
}
