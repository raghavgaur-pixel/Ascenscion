package com.ascension.combat.event;

import com.ascension.combat.model.CombatContext;
import com.ascension.events.AscensionEvent;

import java.util.Objects;

/**
 * Fired after damage has been calculated, but before it is applied to health.
 * Provides the single source of truth context for this interaction.
 */
public class DamageCalculatedEvent implements AscensionEvent {

    private final CombatContext context;

    public DamageCalculatedEvent(final CombatContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public CombatContext context() {
        return this.context;
    }
}
