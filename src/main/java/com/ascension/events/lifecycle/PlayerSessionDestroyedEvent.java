package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published after a session has been disposed.
 */
public final class PlayerSessionDestroyedEvent extends AbstractPlayerSessionEvent {

    public PlayerSessionDestroyedEvent(final PlayerSession session) {
        super(session);
    }
}

