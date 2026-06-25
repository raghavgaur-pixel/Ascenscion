package com.ascension.runtime.tick;

/**
 * Immutable per-tick execution context.
 *
 * @param tickNumber sequential tick number
 * @param timestampMillis wall-clock timestamp in millis
 */
public record TickContext(long tickNumber, long timestampMillis) {
}

