package com.ascension.combat.event;

import com.ascension.combat.model.CombatContext;
import com.ascension.combat.model.HitResult;
import com.ascension.events.AscensionEvent;

import java.util.Objects;

/**
 * Fired when an attack has been fully resolved (after all damage and effects).
 */
public class AttackResolvedEvent implements AscensionEvent {

    private final CombatContext context;
    private final HitResult result;

    public AttackResolvedEvent(final CombatContext context, final HitResult result) {
        this.context = Objects.requireNonNull(context, "context");
        this.result = Objects.requireNonNull(result, "result");
    }

    public CombatContext context() {
        return this.context;
    }

    public HitResult result() {
        return this.result;
    }
}
