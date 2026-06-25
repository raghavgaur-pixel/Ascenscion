package com.ascension.events.lifecycle;

import com.ascension.events.AscensionEvent;
import com.ascension.session.model.PlayerSession;
import java.util.Objects;

/**
 * Base event carrying a player session.
 */
public abstract class AbstractPlayerSessionEvent implements AscensionEvent {

    private final PlayerSession session;

    protected AbstractPlayerSessionEvent(final PlayerSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * @return affected player session
     */
    public PlayerSession session() {
        return this.session;
    }
}

