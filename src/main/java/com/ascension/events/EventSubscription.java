package com.ascension.events;

/**
 * Handle for a registered event listener.
 */
public interface EventSubscription extends AutoCloseable {

    /**
     * Unregisters the listener.
     */
    void unsubscribe();

    @Override
    default void close() {
        this.unsubscribe();
    }
}

