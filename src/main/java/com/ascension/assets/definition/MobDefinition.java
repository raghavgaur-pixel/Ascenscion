package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable definition for a regular combat creature.
 *
 * <p>Bosses intentionally remain a separate asset type because boss-specific
 * phase orchestration and encounter rules should not leak into ordinary mobs.</p>
 */
public record MobDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "mobs";
    }
}
