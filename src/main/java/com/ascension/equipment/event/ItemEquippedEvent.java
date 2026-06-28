package com.ascension.equipment.event;

import com.ascension.equipment.model.EquipmentSlot;
import com.ascension.events.lifecycle.AbstractPlayerSessionEvent;
import com.ascension.items.runtime.AscensionItem;
import com.ascension.session.model.PlayerSession;
import java.util.Objects;

/**
 * Fired when an item is equipped.
 */
public final class ItemEquippedEvent extends AbstractPlayerSessionEvent {

    private final EquipmentSlot slot;
    private final AscensionItem item;

    public ItemEquippedEvent(final PlayerSession session, final EquipmentSlot slot, final AscensionItem item) {
        super(session);
        this.slot = Objects.requireNonNull(slot, "slot");
        this.item = Objects.requireNonNull(item, "item");
    }

    public EquipmentSlot slot() {
        return this.slot;
    }

    public AscensionItem item() {
        return this.item;
    }
}
