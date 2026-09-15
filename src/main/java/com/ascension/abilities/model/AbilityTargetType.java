package com.ascension.abilities.model;

/**
 * High-level targeting modes supported by an ability.
 *
 * <p>Concrete targeting rules are resolved by the execution layer; this enum
 * is only the stable content/API vocabulary.</p>
 */
public enum AbilityTargetType {
    SELF,
    SINGLE_ENTITY,
    AREA,
    CONE,
    LINE,
    PROJECTILE,
    GROUND,
    NONE
}
