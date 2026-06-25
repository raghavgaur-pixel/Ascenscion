package com.ascension.events.lifecycle;

import com.ascension.session.model.PlayerSession;

/**
 * Published after a player profile has been saved during session teardown.
 */
public final class PlayerSessionSavedEvent extends AbstractPlayerSessionEvent {

    public PlayerSessionSavedEvent(final PlayerSession session) {
        super(session);
    }
}

