package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable profession data definition.
 */
public record ProfessionDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "professions";
    }
}

