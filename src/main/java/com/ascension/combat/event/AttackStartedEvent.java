package com.ascension.combat.event;

import com.ascension.combat.model.AttackContext;
import com.ascension.events.AbstractCancellableEvent;

import java.util.Objects;

/**
 * Fired when an attack starts, before any damage calculations.
 */
public class AttackStartedEvent extends AbstractCancellableEvent {

    private final AttackContext context;

    public AttackStartedEvent(final AttackContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public AttackContext context() {
        return this.context;
    }
}
