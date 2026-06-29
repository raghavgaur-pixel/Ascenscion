package com.ascension.combat.event;

import com.ascension.combat.model.CombatContext;
import com.ascension.events.AscensionEvent;

import java.util.Objects;

/**
 * Fired right after damage has been successfully applied to the entity's health.
 */
public class DamageAppliedEvent implements AscensionEvent {

    private final CombatContext context;

    public DamageAppliedEvent(final CombatContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public CombatContext context() {
        return this.context;
    }
}
