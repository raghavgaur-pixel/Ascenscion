package com.ascension.combat.model;

import java.util.Objects;
import java.util.Optional;

/**
 * The single source of truth for a combat transaction.
 * It contains the initial attack context, immutable snapshots of the entities involved,
 * and the progressively calculated damage result.
 */
public class CombatContext {

    private final AttackContext attackContext;
    private final Optional<CombatSnapshot> attackerSnapshot;
    private final CombatSnapshot targetSnapshot;

    private DamageResult currentDamageResult;

    public CombatContext(
        final AttackContext attackContext,
        final Optional<CombatSnapshot> attackerSnapshot,
        final CombatSnapshot targetSnapshot
    ) {
        this.attackContext = Objects.requireNonNull(attackContext, "attackContext");
        this.attackerSnapshot = Objects.requireNonNull(attackerSnapshot, "attackerSnapshot");
        this.targetSnapshot = Objects.requireNonNull(targetSnapshot, "targetSnapshot");

        // Initialize with base damage
        this.currentDamageResult = new DamageResult(
            attackContext.damageContext(),
            attackContext.damageContext().baseAmount(),
            0.0,
            false
        );
    }

    public AttackContext attackContext() {
        return this.attackContext;
    }

    public Optional<CombatSnapshot> attackerSnapshot() {
        return this.attackerSnapshot;
    }

    public CombatSnapshot targetSnapshot() {
        return this.targetSnapshot;
    }

    public DamageResult currentDamageResult() {
        return this.currentDamageResult;
    }

    public void setCurrentDamageResult(final DamageResult result) {
        this.currentDamageResult = Objects.requireNonNull(result, "result");
    }
}
