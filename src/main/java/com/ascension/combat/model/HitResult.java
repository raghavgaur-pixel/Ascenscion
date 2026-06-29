package com.ascension.combat.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Result representing the full resolution of an attack.
 *
 * @param attackContext The context of the attack.
 * @param damageResult The resulting damage calculation, if any.
 * @param wasKilled True if the target was killed by this hit.
 * @param wasCancelled True if the attack was cancelled before resolution.
 */
public record HitResult(
    AttackContext attackContext,
    Optional<DamageResult> damageResult,
    boolean wasKilled,
    boolean wasCancelled
) {
    public HitResult {
        Objects.requireNonNull(attackContext, "attackContext");
        Objects.requireNonNull(damageResult, "damageResult");
    }

    /**
     * @return true if the attack was successful (not cancelled).
     */
    public boolean isSuccess() {
        return !wasCancelled;
    }
}
