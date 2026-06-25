package com.ascension.runtime.tick;

import java.util.Collection;

/**
 * Registry and monitoring service for centralized tick tasks.
 */
public interface TickManager {

    /**
     * Registers a tick task.
     *
     * @param task tick task
     * @return registration handle
     */
    TickRegistration register(TickTask task);

    /**
     * Removes every tick task owned by the given owner.
     *
     * @param owner owner identifier
     */
    void unregisterOwner(String owner);

    /**
     * @return immutable snapshot of metrics
     */
    Collection<TickMetrics> metrics();
}

