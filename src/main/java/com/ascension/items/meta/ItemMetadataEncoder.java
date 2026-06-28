package com.ascension.items.meta;

import com.ascension.assets.model.AssetId;
import com.ascension.items.runtime.AscensionItem;
import java.util.Optional;

public interface ItemMetadataEncoder {

    void encode(AscensionItem item, Object platformItem);

    Optional<AssetId> decodeDefinitionId(Object platformItem);

    Optional<java.util.UUID> decodeInstanceId(Object platformItem);
}
