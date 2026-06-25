package com.ascension.stats.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable stat definition loaded through the asset framework.
 *
 * @param descriptor common asset descriptor
 * @param category stat category
 * @param defaultBaseValue default runtime base value
 * @param minimumValue optional lower clamp
 * @param maximumValue optional upper clamp
 * @param decimalPlaces recommended decimal precision
 * @param formula optional derived formula
 * @param data raw asset payload
 */
public record StatDefinition(
    AssetDescriptor descriptor,
    StatCategory category,
    double defaultBaseValue,
    Optional<Double> minimumValue,
    Optional<Double> maximumValue,
    int decimalPlaces,
    Optional<DerivedFormulaDefinition> formula,
    SerializedObject data
) implements AssetDefinition {

    public StatDefinition {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(minimumValue, "minimumValue");
        Objects.requireNonNull(maximumValue, "maximumValue");
        Objects.requireNonNull(formula, "formula");
        Objects.requireNonNull(data, "data");
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("decimalPlaces must be non-negative.");
        }
    }

    /**
     * @return {@code true} when this stat is derived from other stats
     */
    public boolean derived() {
        return this.formula.isPresent();
    }

    @Override
    public String type() {
        return "stats";
    }
}
