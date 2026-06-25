package com.ascension.runtime.tick;

import java.util.Objects;

/**
 * Registered tick task owned by a runtime subsystem.
 *
 * @param owner owner identifier
 * @param name task name
 * @param priority execution priority
 * @param tickable tick callback
 */
public record TickTask(
    String owner,
    String name,
    TickPriority priority,
    Tickable tickable
) {

    public TickTask {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(tickable, "tickable");
    }
}

