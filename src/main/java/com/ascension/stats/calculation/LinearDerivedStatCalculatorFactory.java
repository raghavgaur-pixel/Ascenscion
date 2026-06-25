package com.ascension.stats.calculation;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.definition.DerivedFormulaDefinition;
import com.ascension.stats.definition.StatDefinition;
import java.util.Objects;
import java.util.Set;

/**
 * Factory for linear derived formulas loaded from stat assets.
 */
public final class LinearDerivedStatCalculatorFactory implements DerivedStatCalculatorFactory {

    @Override
    public String type() {
        return "linear";
    }

    @Override
    public DerivedStatCalculator create(final StatDefinition definition) {
        final DerivedFormulaDefinition formula = definition.formula()
            .orElseThrow(() -> new IllegalArgumentException("Missing formula for derived stat " + definition.id()));
        if (!this.type().equalsIgnoreCase(formula.type())) {
            throw new IllegalArgumentException(
                "Unsupported formula type '" + formula.type() + "' for calculator factory '" + this.type() + "'."
            );
        }
        return new DerivedStatCalculator() {
            @Override
            public String id() {
                return "linear:" + definition.id();
            }

            @Override
            public AssetId targetStatId() {
                return definition.id();
            }

            @Override
            public Set<AssetId> dependencies() {
                return formula.coefficients().keySet();
            }

            @Override
            public double calculate(final DerivedStatContext context) {
                double value = formula.baseValue();
                for (final java.util.Map.Entry<AssetId, Double> entry : formula.coefficients().entrySet()) {
                    value += context.finalValue(entry.getKey()) * entry.getValue();
                }
                return value;
            }
        };
    }
}
