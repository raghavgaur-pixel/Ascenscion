package com.ascension.stats.attribute;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.modifier.AttributeModifier;
import java.util.Collection;
import java.util.Optional;

/**
 * Mutable runtime attribute container owned by a player session.
 */
public interface AttributeContainer extends AttributeContainerView {

    /**
     * Sets the base value of a stat.
     *
     * @param statId stat id
     * @param value new base value
     */
    void setBaseValue(AssetId statId, double value);

    /**
     * Adds a modifier.
     *
     * @param statId stat id
     * @param modifier modifier to add
     */
    void addModifier(AssetId statId, AttributeModifier modifier);

    /**
     * Removes a modifier by id.
     *
     * @param statId stat id
     * @param modifierId modifier id
     * @return {@code true} if removed
     */
    boolean removeModifier(AssetId statId, String modifierId);

    /**
     * Removes all modifiers produced by a source id.
     *
     * @param sourceId source id
     */
    void removeSource(String sourceId);

    /**
     * Reads active modifiers for a stat.
     *
     * @param statId stat id
     * @return immutable modifier snapshot
     */
    Collection<AttributeModifier> modifiers(AssetId statId);

    /**
     * Checks whether a stat is dirty.
     *
     * @param statId stat id
     * @return {@code true} if dirty
     */
    boolean dirty(AssetId statId);

    /**
     * Registers a stat definition inside the container.
     *
     * @param statId stat id
     * @return definition if present
     */
    Optional<com.ascension.stats.definition.StatDefinition> register(AssetId statId);

    /**
     * Clears runtime state.
     */
    void clear();
}
