package com.ascension.items.definition;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.loader.AssetSerializer;
import com.ascension.assets.loader.AssetSource;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.items.component.ItemComponent;
import com.ascension.serialization.SerializedObject;
import java.util.List;

public final class ItemDefinitionSerializer implements AssetSerializer<ItemDefinition> {

    @Override
    public ItemDefinition deserialize(AssetSource source, SerializedObject object, String owner) {
        AssetDescriptor descriptor = new AssetDescriptor(
            AssetId.parse(object.getString("id", "")),
            owner,
            SemanticVersion.parse(object.getString("version", "1.0.0")),
            object.getString("display_name", ""),
            object.getString("description", ""),
            object.getStringMap("metadata"),
            AssetCompatibility.open(),
            java.util.Set.of()
        );

        // Future systems will dynamically parse components here
        return new ItemDefinition(descriptor, List.of(), object);
    }

    @Override
    public SerializedObject serialize(ItemDefinition definition) {
        return definition.data();
    }
}
