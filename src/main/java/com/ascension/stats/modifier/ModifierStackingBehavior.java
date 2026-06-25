package com.ascension.stats.modifier;

/**
 * Policy used when another modifier from the same logical source is added.
 */
public enum ModifierStackingBehavior {
    STACK,
    UNIQUE_PER_SOURCE,
    REPLACE_BY_ID,
    HIGHEST_VALUE,
    REFRESH_DURATION
}
