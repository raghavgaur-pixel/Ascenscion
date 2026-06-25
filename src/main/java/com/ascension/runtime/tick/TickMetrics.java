package com.ascension.runtime.tick;

/**
 * Snapshot of per-task tick performance metrics.
 *
 * @param owner owner identifier
 * @param name task name
 * @param invocations invocation count
 * @param lastDurationNanos last execution time
 * @param maxDurationNanos max execution time
 */
public record TickMetrics(
    String owner,
    String name,
    long invocations,
    long lastDurationNanos,
    long maxDurationNanos
) {
}

