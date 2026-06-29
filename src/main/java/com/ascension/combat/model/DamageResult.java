package com.ascension.combat.model;

import java.util.Objects;

/**
 * Result of calculating damage through the pipeline.
 *
 * @param damageContext The context describing the initial damage.
 * @param finalDamage The calculated final damage amount.
 * @param mitigated The amount of damage mitigated.
 * @param isCritical True if the damage was a critical hit.
 */
public record DamageResult(
    DamageContext damageContext,
    double finalDamage,
    double mitigated,
    boolean isCritical
) {
    public DamageResult {
        Objects.requireNonNull(damageContext, "damageContext");
    }
}
