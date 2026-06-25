package com.ascension.stats.attribute;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.modifier.AttributeModifier;
import java.util.List;
import java.util.Objects;

/**
 * Immutable read model for a single resolved attribute value.
 *
 * @param statId stat id
 * @param baseValue base value before modifiers
 * @param finalValue resolved final value
 * @param dirty whether the cached value was dirty at snapshot time
 * @param modifiers active modifiers
 */
public record AttributeValueSnapshot(
    AssetId statId,
    double baseValue,
    double finalValue,
    boolean dirty,
    List<AttributeModifier> modifiers
) {

    public AttributeValueSnapshot {
        Objects.requireNonNull(statId, "statId");
        modifiers = List.copyOf(modifiers);
    }
}
