package com.ascension.stats.calculation;

import com.ascension.assets.model.AssetId;

/**
 * Read-only context exposed to derived stat calculators.
 */
public interface DerivedStatContext {

    /**
     * Resolves the fully calculated value of another stat.
     *
     * @param statId source stat id
     * @return resolved final value
     */
    double finalValue(AssetId statId);
}
