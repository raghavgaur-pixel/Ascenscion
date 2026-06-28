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
import com.ascension.items.component.StatModifierComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        List<ItemComponent> components = new ArrayList<>();
        Map<String, Long> rawStatLines = object.getLongMap("stats");
        if (!rawStatLines.isEmpty()) {
            Map<AssetId, Double> modifiers = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : rawStatLines.entrySet()) {
                modifiers.put(AssetId.parse(entry.getKey()), entry.getValue().doubleValue());
            }
            components.add(new StatModifierComponent(modifiers));
        }

        // Future systems will dynamically parse more components here
        return new ItemDefinition(descriptor, components, object);
    }

    @Override
    public SerializedObject serialize(ItemDefinition definition) {
        return definition.data();
    }
}
