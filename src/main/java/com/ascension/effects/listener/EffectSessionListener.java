package com.ascension.effects.listener;

import com.ascension.effects.service.EffectService;
import com.ascension.events.EventBus;
import com.ascension.events.EventPriority;
import com.ascension.events.lifecycle.PlayerSessionDestroyedEvent;
import com.ascension.events.lifecycle.PlayerSessionCreatedEvent;

import java.util.Objects;

/**
 * Listens for session lifecycle events to manage effects container state.
 */
public final class EffectSessionListener {

    private final EffectService effectService;

    public EffectSessionListener(final EventBus eventBus, final EffectService effectService) {
        Objects.requireNonNull(eventBus, "eventBus");
        this.effectService = Objects.requireNonNull(effectService, "effectService");

        eventBus.subscribe(
            "effects",
            PlayerSessionDestroyedEvent.class,
            EventPriority.NORMAL,
            false,
            this::onSessionDestroyed
        );

        eventBus.subscribe(
            "effects",
            PlayerSessionCreatedEvent.class,
            EventPriority.NORMAL,
            false,
            this::onSessionCreated
        );
    }

    private void onSessionCreated(final PlayerSessionCreatedEvent event) {
        // Prepare effect container for a fresh session, handled inside PlayerSession itself primarily
    }

    private void onSessionDestroyed(final PlayerSessionDestroyedEvent event) {
        this.effectService.clearEffects(event.session());
    }
}
