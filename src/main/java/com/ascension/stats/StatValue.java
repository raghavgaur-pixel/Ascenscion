package com.ascension.stats;

import java.util.Objects;

/**
 * Immutable value for a single statistic.
 *
 * <p>Stat values are intentionally numeric and unit-agnostic. Interpretation
 * such as percentage points versus multipliers is owned by the stat formula
 * layer.</p>
 */
public record StatValue(StatType type, double value) {

    public StatValue {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Stat value must be finite");
        }
    }
}
