package com.ascension.abilities.model;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;
import java.util.Objects;

/**
 * Immutable, data-driven ability definition.
 *
 * <p>The definition describes what an ability is; execution is deliberately
 * separated so content remains configuration-driven and safe to reload.</p>
 */
public record AbilityDefinition(
    AssetDescriptor descriptor,
    AbilityTargetType targetType,
    AbilityCost cost,
    long cooldownMillis,
    SerializedObject data
) implements AssetDefinition {

    public AbilityDefinition {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(data, "data");
        if (cooldownMillis < 0L) {
            throw new IllegalArgumentException("cooldownMillis cannot be negative");
        }
    }

    @Override
    public String type() {
        return "abilities";
    }
}
