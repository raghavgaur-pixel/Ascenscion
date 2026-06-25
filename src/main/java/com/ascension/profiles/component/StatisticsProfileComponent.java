package com.ascension.profiles.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks generic numeric player statistics.
 */
public final class StatisticsProfileComponent implements ProfileComponent {

    private final Map<String, Long> counters;

    public StatisticsProfileComponent() {
        this.counters = new ConcurrentHashMap<>();
    }

    public StatisticsProfileComponent(final Map<String, Long> counters) {
        this.counters = new ConcurrentHashMap<>(counters);
    }

    public long get(final String statisticId) {
        return this.counters.getOrDefault(statisticId, 0L);
    }

    public long increment(final String statisticId, final long amount) {
        return this.counters.merge(statisticId, amount, Long::sum);
    }

    public Map<String, Long> snapshot() {
        return Map.copyOf(this.counters);
    }
}

