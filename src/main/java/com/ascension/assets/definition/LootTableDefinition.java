package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;

/**
 * Immutable loot table data definition.
 */
public record LootTableDefinition(AssetDescriptor descriptor, SerializedObject data) implements AssetDefinition {

    @Override
    public String type() {
        return "loot_tables";
    }
}

