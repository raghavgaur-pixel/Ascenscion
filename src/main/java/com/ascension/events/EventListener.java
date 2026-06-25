package com.ascension.events;

/**
 * Listener callback for internal events.
 *
 * @param <T> event type
 */
@FunctionalInterface
public interface EventListener<T extends AscensionEvent> {

    /**
     * Handles an event.
     *
     * @param event event instance
     */
    void handle(T event);
}

