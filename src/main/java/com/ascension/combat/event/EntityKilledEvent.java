package com.ascension.combat.event;

import com.ascension.combat.model.CombatContext;
import com.ascension.combat.model.CombatEntity;
import com.ascension.events.AscensionEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Fired when an entity's health reaches zero as a result of an attack.
 */
public class EntityKilledEvent implements AscensionEvent {

    private final CombatEntity entity;
    private final CombatContext fatalContext;

    public EntityKilledEvent(final CombatEntity entity, final CombatContext fatalContext) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.fatalContext = Objects.requireNonNull(fatalContext, "fatalContext");
    }

    public CombatEntity entity() {
        return this.entity;
    }

    public Optional<CombatEntity> killer() {
        return this.fatalContext.attackContext().attacker();
    }

    public CombatContext fatalContext() {
        return this.fatalContext;
    }
}
