package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published when a runtime session object is created.
 */
public final class PlayerSessionCreatedEvent extends AbstractPlayerSessionEvent {

    public PlayerSessionCreatedEvent(final PlayerSession session) {
        super(session);
    }
}

