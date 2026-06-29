package com.ascension.effects.model;

/**
 * Policy used when another instance of the same effect is applied.
 */
public enum EffectStackingBehavior {
    REFRESH_DURATION,
    REPLACE_EXISTING,
    INDEPENDENT_STACKS,
    MAXIMUM_STACKS,
    PRIORITY_OVERRIDE,
    CUSTOM_MERGE_STRATEGY
}
