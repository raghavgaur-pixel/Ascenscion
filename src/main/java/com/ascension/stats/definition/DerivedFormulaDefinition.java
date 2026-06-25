package com.ascension.stats.definition;

import com.ascension.assets.model.AssetId;
import java.util.Map;
import java.util.Objects;

/**
 * Asset-driven derived stat formula definition.
 *
 * @param type formula type identifier
 * @param baseValue constant base contribution
 * @param coefficients dependency coefficients keyed by source stat id
 */
public record DerivedFormulaDefinition(
    String type,
    double baseValue,
    Map<AssetId, Double> coefficients
) {

    public DerivedFormulaDefinition {
        Objects.requireNonNull(type, "type");
        coefficients = Map.copyOf(coefficients);
    }
}
