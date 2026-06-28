package com.ascension.equipment.model;

import com.ascension.items.runtime.AscensionItem;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable snapshot of currently equipped items.
 */
public record EquipmentSnapshot(Map<EquipmentSlot, AscensionItem> items) {

    public EquipmentSnapshot {
        items = Map.copyOf(items);
    }

    public Optional<AscensionItem> item(final EquipmentSlot slot) {
        return Optional.ofNullable(this.items.get(slot));
    }
}
