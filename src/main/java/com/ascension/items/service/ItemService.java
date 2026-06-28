package com.ascension.items.service;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.items.runtime.AscensionItem;
import java.util.Optional;

public interface ItemService {

    Optional<ItemDefinition> findDefinition(AssetId id);

    AscensionItem create(AssetId id);
}
