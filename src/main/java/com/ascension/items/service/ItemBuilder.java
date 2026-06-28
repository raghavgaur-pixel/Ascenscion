package com.ascension.items.service;

import com.ascension.assets.model.AssetId;
import com.ascension.items.runtime.AscensionItem;

public final class ItemBuilder {

    private final ItemService itemService;
    private final AssetId definitionId;

    public ItemBuilder(ItemService itemService, AssetId definitionId) {
        this.itemService = itemService;
        this.definitionId = definitionId;
    }

    public AscensionItem build() {
        return itemService.create(definitionId);
    }
}
