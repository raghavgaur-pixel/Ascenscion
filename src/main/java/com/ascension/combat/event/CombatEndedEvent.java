package com.ascension.combat.event;

import com.ascension.combat.model.CombatEntity;
import com.ascension.events.AscensionEvent;

import java.util.Objects;

/**
 * Fired when an entity's combat state formally ends.
 */
public class CombatEndedEvent implements AscensionEvent {

    private final CombatEntity entity;

    public CombatEndedEvent(final CombatEntity entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
    }

    public CombatEntity entity() {
        return this.entity;
    }
}
