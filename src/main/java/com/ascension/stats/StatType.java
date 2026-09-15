package com.ascension.stats;

/**
 * Canonical character and combat statistics used by Ascension.
 *
 * <p>The enum deliberately contains no balancing values. Base values and derived
 * formulas belong to progression/configuration layers so server owners can tune
 * them without changing the API.</p>
 */
public enum StatType {
    HEALTH,
    HEALTH_REGEN,
    MANA,
    MANA_REGEN,
    STRENGTH,
    VITALITY,
    AGILITY,
    INTELLIGENCE,
    DEFENSE,
    MAGIC_RESISTANCE,
    ATTACK_DAMAGE,
    SPELL_POWER,
    ATTACK_SPEED,
    CAST_SPEED,
    CRIT_CHANCE,
    CRIT_DAMAGE,
    ARMOR_PENETRATION,
    MAGIC_PENETRATION,
    ACCURACY,
    EVASION,
    MOVEMENT_SPEED,
    LIFESTEAL,
    TENACITY
}
