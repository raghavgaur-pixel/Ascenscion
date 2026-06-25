package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable floor data definition.
 */
public record FloorDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "floors";
    }
}

