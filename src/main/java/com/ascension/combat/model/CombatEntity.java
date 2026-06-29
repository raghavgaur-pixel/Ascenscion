package com.ascension.combat.model;

import com.ascension.effects.runtime.EffectContainer;
import com.ascension.stats.attribute.AttributeContainer;
import java.util.UUID;

/**
 * An entity that can participate in combat (has health, stats, and effects).
 */
public interface CombatEntity {

    /**
     * @return The unique identifier of the entity.
     */
    UUID uniqueId();

    /**
     * @return The name of the entity.
     */
    String name();

    /**
     * @return The runtime health state of the entity.
     */
    RuntimeHealth health();

    /**
     * @return The attribute container storing the entity's stats.
     */
    AttributeContainer attributes();

    /**
     * @return The container holding the entity's active effects.
     */
    EffectContainer effects();
}
