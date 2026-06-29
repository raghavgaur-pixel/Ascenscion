package com.ascension.effects.event;

import com.ascension.effects.runtime.EffectInstance;
import com.ascension.events.lifecycle.AbstractPlayerSessionEvent;
import com.ascension.session.model.PlayerSession;

import java.util.Objects;

/**
 * Fired when an effect's stack count changes.
 */
public final class EffectStackChangedEvent extends AbstractPlayerSessionEvent {

    private final EffectInstance effect;
    private final int previousStacks;
    private final int newStacks;

    public EffectStackChangedEvent(
        final PlayerSession session,
        final EffectInstance effect,
        final int previousStacks,
        final int newStacks
    ) {
        super(session);
        this.effect = Objects.requireNonNull(effect, "effect");
        this.previousStacks = previousStacks;
        this.newStacks = newStacks;
    }

    public EffectInstance effect() {
        return this.effect;
    }

    public int previousStacks() {
        return this.previousStacks;
    }

    public int newStacks() {
        return this.newStacks;
    }
}
