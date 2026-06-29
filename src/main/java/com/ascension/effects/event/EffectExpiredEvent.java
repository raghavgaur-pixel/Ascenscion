package com.ascension.effects.event;

import com.ascension.effects.runtime.EffectInstance;
import com.ascension.events.lifecycle.AbstractPlayerSessionEvent;
import com.ascension.session.model.PlayerSession;

import java.util.Objects;

/**
 * Fired specifically when an effect expires naturally.
 */
public final class EffectExpiredEvent extends AbstractPlayerSessionEvent {

    private final EffectInstance effect;

    public EffectExpiredEvent(final PlayerSession session, final EffectInstance effect) {
        super(session);
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    public EffectInstance effect() {
        return this.effect;
    }
}
