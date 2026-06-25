package com.ascension.assets.model;

import java.util.Objects;

/**
 * Typed reference to another asset.
 *
 * @param type target asset type identifier
 * @param id target asset id
 */
public record AssetReference(String type, AssetId id) {

    public AssetReference {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }
}

