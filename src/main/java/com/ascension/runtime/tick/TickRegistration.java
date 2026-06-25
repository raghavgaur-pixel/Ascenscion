package com.ascension.runtime.tick;

/**
 * Handle to an active tick registration.
 */
public interface TickRegistration extends AutoCloseable {

    /**
     * Unregisters the tick task.
     */
    void unregister();

    @Override
    default void close() {
        this.unregister();
    }
}

