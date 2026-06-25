package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published after a profile has been attached to a session.
 */
public final class PlayerSessionLoadedEvent extends AbstractPlayerSessionEvent {

    public PlayerSessionLoadedEvent(final PlayerSession session) {
        super(session);
    }
}

