package com.ascension.stats.calculation;

import com.ascension.assets.model.AssetId;
import java.util.Set;

/**
 * Calculator for a derived stat definition.
 */
public interface DerivedStatCalculator {

    /**
     * @return stable calculator id
     */
    String id();

    /**
     * @return target stat id
     */
    AssetId targetStatId();

    /**
     * @return immutable dependency set
     */
    Set<AssetId> dependencies();

    /**
     * Calculates the derived base value.
     *
     * @param context dependency resolution context
     * @return calculated value
     */
    double calculate(DerivedStatContext context);
}
