package com.ascension.items.service;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.items.runtime.AscensionItem;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import java.util.Optional;
import java.util.UUID;

public final class DefaultItemService implements ItemService {

    private final RegistryHub registryHub;

    public DefaultItemService(RegistryHub registryHub) {
        this.registryHub = registryHub;
    }

    @Override
    public Optional<ItemDefinition> findDefinition(AssetId id) {
        return registryHub.getOrCreate(AscensionRegistries.ITEM_DEFINITIONS).find(id);
    }

    @Override
    public AscensionItem create(AssetId id) {
        if (findDefinition(id).isEmpty()) {
            throw new IllegalArgumentException("Unknown item: " + id);
        }
        return new AscensionItem(UUID.randomUUID(), id);
    }
}
