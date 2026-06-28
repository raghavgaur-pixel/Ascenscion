package com.ascension.items.runtime;

import com.ascension.assets.model.AssetId;
import java.util.UUID;

public final class AscensionItem {

    private final UUID instanceId;
    private final AssetId definitionId;

    public AscensionItem(UUID instanceId, AssetId definitionId) {
        this.instanceId = instanceId;
        this.definitionId = definitionId;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public AssetId definitionId() {
        return definitionId;
    }
}
