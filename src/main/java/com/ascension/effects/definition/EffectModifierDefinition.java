package com.ascension.effects.definition;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.modifier.ModifierOperation;
import java.util.Objects;

/**
 * Definition of a stat modifier applied by an effect.
 *
 * @param statId the stat to modify
 * @param operation arithmetic operation
 * @param value modifier value
 */
public record EffectModifierDefinition(
    AssetId statId,
    ModifierOperation operation,
    double value
) {

    public EffectModifierDefinition {
        Objects.requireNonNull(statId, "statId");
        Objects.requireNonNull(operation, "operation");
    }
}
