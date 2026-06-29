package com.ascension.combat.model;

import com.ascension.effects.runtime.EffectInstance;
import com.ascension.stats.attribute.AttributeSnapshot;

import java.util.Collection;
import java.util.List;

/**
 * Immutable snapshot of an entity's combat-relevant state at the time of calculation.
 *
 * @param health The snapshot of the current health value.
 * @param maxHealth The snapshot of the max health value.
 * @param attributes The snapshot of the entity's attributes.
 * @param effects The snapshot of the entity's active effects.
 */
public record CombatSnapshot(
    double health,
    double maxHealth,
    AttributeSnapshot attributes,
    Collection<EffectInstance> effects
) {
    public CombatSnapshot {
        effects = List.copyOf(effects);
    }
}
