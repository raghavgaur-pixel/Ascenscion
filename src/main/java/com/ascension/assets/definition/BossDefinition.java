package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable boss data definition.
 */
public record BossDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "bosses";
    }
}

