package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published when a player starts leaving and cleanup begins.
 */
public final class PlayerLeavingEvent extends AbstractPlayerSessionEvent {

    public PlayerLeavingEvent(final PlayerSession session) {
        super(session);
    }
}

