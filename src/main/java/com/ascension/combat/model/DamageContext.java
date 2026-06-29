package com.ascension.combat.model;

import java.util.Objects;

/**
 * Information describing a requested instance of damage.
 *
 * @param source The source that caused the damage.
 * @param type The type of the damage.
 * @param baseAmount The base amount of damage requested before mitigation or modifiers.
 */
public record DamageContext(
    DamageSource source,
    DamageType type,
    double baseAmount
) {
    public DamageContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        if (baseAmount < 0) {
            throw new IllegalArgumentException("baseAmount cannot be negative");
        }
    }
}
