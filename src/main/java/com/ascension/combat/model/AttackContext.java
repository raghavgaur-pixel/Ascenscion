package com.ascension.combat.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Context for an attack interaction.
 *
 * @param attacker The entity attacking (if applicable).
 * @param target The target being attacked.
 * @param damageContext The context describing the damage to be dealt.
 */
public record AttackContext(
    Optional<CombatEntity> attacker,
    CombatTarget target,
    DamageContext damageContext
) {
    public AttackContext {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(damageContext, "damageContext");
    }
}
