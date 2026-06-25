package com.ascension.runtime.tick;

/**
 * Lifecycle-aware centralized game loop service.
 */
public interface GameLoop extends TickManager {

    /**
     * Starts the loop.
     */
    void start();

    /**
     * Stops the loop and clears active loop tasks.
     */
    void stop();
}

