package com.ascension.runtime.tick;

/**
 * Contract for systems that participate in the centralized game loop.
 */
@FunctionalInterface
public interface Tickable {

    /**
     * Executes one tick.
     *
     * @param context tick context
     */
    void tick(TickContext context);
}

