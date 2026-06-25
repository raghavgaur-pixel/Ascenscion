package com.ascension.assets.registry;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.Registry;
import java.util.Collection;

/**
 * Asset-specific registry view with namespace lookup.
 *
 * @param <T> asset type
 */
public interface AssetRegistry<T extends AssetDefinition> extends Registry<AssetId, T> {

    /**
     * Finds all assets in a namespace.
     *
     * @param namespace namespace
     * @return matching assets
     */
    Collection<T> namespace(String namespace);
}

