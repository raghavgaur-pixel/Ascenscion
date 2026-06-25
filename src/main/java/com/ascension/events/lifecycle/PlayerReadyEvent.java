package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published when a session is fully ready for gameplay systems.
 */
public final class PlayerReadyEvent extends AbstractPlayerSessionEvent {

    public PlayerReadyEvent(final PlayerSession session) {
        super(session);
    }
}

