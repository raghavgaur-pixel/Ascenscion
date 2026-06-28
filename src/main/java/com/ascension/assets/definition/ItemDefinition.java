package com.ascension.assets.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.items.component.ItemComponent;
import com.ascension.serialization.SerializedObject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Immutable item data definition using composition.
 */
public record ItemDefinition(
    AssetDescriptor descriptor,
    Collection<ItemComponent> components,
    SerializedObject rawData
) implements AssetDefinition {

    public ItemDefinition {
        components = List.copyOf(components);
    }

    @Override
    public String type() {
        return "items";
    }

    @Override
    public SerializedObject data() {
        return this.rawData;
    }

    public <T extends ItemComponent> Optional<T> component(Class<T> type) {
        return components.stream()
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst();
    }
}

