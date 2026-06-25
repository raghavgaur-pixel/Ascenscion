package com.ascension.events;

/**
 * Base implementation for cancellable events.
 */
public abstract class AbstractCancellableEvent implements CancellableEvent {

    private volatile boolean cancelled;

    @Override
    public final boolean cancelled() {
        return this.cancelled;
    }

    @Override
    public final void cancel() {
        this.cancelled = true;
    }
}

