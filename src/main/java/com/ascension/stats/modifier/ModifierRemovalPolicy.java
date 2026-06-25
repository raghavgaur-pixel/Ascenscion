package com.ascension.stats.modifier;

/**
 * Lifecycle policy for modifier removal.
 */
public enum ModifierRemovalPolicy {
    MANUAL,
    ON_EXPIRE,
    ON_SESSION_END,
    ON_SOURCE_REMOVED
}
