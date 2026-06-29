package com.ascension.combat.model;

import java.util.Optional;

/**
 * First-class runtime object for damage sources.
 * Examples: Player, Mob, Projectile, Ability, Environment, Boss Mechanic, Status Effect, Trap.
 */
public interface DamageSource {

    /**
     * @return The identifier of the damage source.
     */
    String id();

    /**
     * @return The name of the damage source.
     */
    String name();

    /**
     * @return The combat entity that caused the damage, if applicable.
     */
    Optional<CombatEntity> entity();
}
