package com.ascension.equipment.event;

import com.ascension.events.lifecycle.AbstractPlayerSessionEvent;
import com.ascension.session.model.PlayerSession;

/**
 * Fired when a player's equipment changes.
 */
public final class EquipmentChangedEvent extends AbstractPlayerSessionEvent {

    public EquipmentChangedEvent(final PlayerSession session) {
        super(session);
    }
}
