package com.ascension.events;

/**
 * Event contract supporting cancellation.
 */
public interface CancellableEvent extends AscensionEvent {

    /**
     * @return {@code true} if cancelled
     */
    boolean cancelled();

    /**
     * Marks the event as cancelled.
     */
    void cancel();
}

