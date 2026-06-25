package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable skill data definition.
 */
public record SkillDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "skills";
    }
}

