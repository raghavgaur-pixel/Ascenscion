package com.ascension.combat.model;

/**
 * Flexible runtime damage types for combat interactions.
 * Future systems should register additional damage types without modifying the engine.
 */
public interface DamageType {

    /**
     * @return The unique identifier or name of the damage type.
     */
    String name();
}
