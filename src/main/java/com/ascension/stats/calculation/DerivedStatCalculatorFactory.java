package com.ascension.stats.calculation;

import com.ascension.stats.definition.StatDefinition;

/**
 * Factory for building runtime derived calculators from stat asset metadata.
 */
public interface DerivedStatCalculatorFactory {

    /**
     * @return supported formula type identifier
     */
    String type();

    /**
     * Creates a calculator for a stat definition.
     *
     * @param definition stat definition
     * @return runtime calculator
     */
    DerivedStatCalculator create(StatDefinition definition);
}
