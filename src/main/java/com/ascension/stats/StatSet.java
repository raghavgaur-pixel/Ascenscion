package com.ascension.stats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable collection of stat values.
 */
public final class StatSet {

    private final Map<StatType, Double> values;

    private StatSet(final Map<StatType, Double> values) {
        this.values = Map.copyOf(values);
    }

    public static StatSet empty() {
        return new StatSet(Map.of());
    }

    public static StatSet copyOf(final Map<StatType, Double> values) {
        Objects.requireNonNull(values, "values");
        final EnumMap<StatType, Double> copy = new EnumMap<>(StatType.class);
        values.forEach((type, value) -> {
            Objects.requireNonNull(type, "stat type");
            Objects.requireNonNull(value, "stat value");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Stat value must be finite");
            }
            copy.put(type, value);
        });
        return new StatSet(copy);
    }

    public double get(final StatType type) {
        return this.values.getOrDefault(Objects.requireNonNull(type, "type"), 0.0D);
    }

    public boolean contains(final StatType type) {
        return this.values.containsKey(Objects.requireNonNull(type, "type"));
    }

    public Map<StatType, Double> asMap() {
        return this.values;
    }

    public StatSet plus(final StatSet other) {
        Objects.requireNonNull(other, "other");
        final EnumMap<StatType, Double> result = new EnumMap<>(StatType.class);
        result.putAll(this.values);
        other.values.forEach((type, value) -> result.merge(type, value, Double::sum));
        return copyOf(result);
    }
}
