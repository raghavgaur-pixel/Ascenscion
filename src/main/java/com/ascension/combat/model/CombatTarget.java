package com.ascension.combat.model;

import java.util.Objects;

/**
 * A wrapper defining the target of a combat action.
 *
 * @param entity The combat entity targeted.
 */
public record CombatTarget(CombatEntity entity) {
    public CombatTarget {
        Objects.requireNonNull(entity, "entity");
    }
}
