package com.ascension.combat.model;

/**
 * Tracks the runtime health of a combat entity.
 */
public interface RuntimeHealth {

    /**
     * @return Current health value.
     */
    double current();

    /**
     * @return Maximum health value.
     */
    double maximum();

    /**
     * @param amount The new health value.
     */
    void set(double amount);

    /**
     * Heals the entity.
     * @param amount the amount to heal.
     */
    void heal(double amount);

    /**
     * Damages the entity.
     * @param amount the amount to damage.
     */
    void damage(double amount);

    /**
     * @return True if health is greater than 0.
     */
    boolean isAlive();
}
