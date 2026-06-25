package com.ascension.assets.model;

import com.ascension.serialization.SerializedObject;
import java.util.Map;
import java.util.Set;

/**
 * Immutable data-only definition for a loaded asset.
 */
public interface AssetDefinition {

    /**
     * @return definition type identifier
     */
    String type();

    /**
     * @return common asset descriptor
     */
    AssetDescriptor descriptor();

    /**
     * @return asset payload data
     */
    SerializedObject data();

    default AssetId id() {
        return this.descriptor().id();
    }

    default String owner() {
        return this.descriptor().owner();
    }

    default SemanticVersion version() {
        return this.descriptor().version();
    }

    default String displayName() {
        return this.descriptor().displayName();
    }

    default String description() {
        return this.descriptor().description();
    }

    default Map<String, String> metadata() {
        return this.descriptor().metadata();
    }

    default AssetCompatibility compatibility() {
        return this.descriptor().compatibility();
    }

    default Set<AssetReference> dependencies() {
        return this.descriptor().dependencies();
    }
}

