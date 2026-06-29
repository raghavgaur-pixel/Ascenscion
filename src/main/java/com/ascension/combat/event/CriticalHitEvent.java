package com.ascension.combat.event;

import com.ascension.combat.model.CombatContext;
import com.ascension.events.AscensionEvent;

import java.util.Objects;

/**
 * Fired specifically when a calculated attack is a critical hit.
 */
public class CriticalHitEvent implements AscensionEvent {

    private final CombatContext context;

    public CriticalHitEvent(final CombatContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public CombatContext context() {
        return this.context;
    }
}
