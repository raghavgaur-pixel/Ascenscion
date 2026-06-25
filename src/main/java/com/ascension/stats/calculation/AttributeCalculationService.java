package com.ascension.stats.calculation;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.attribute.AttributeContainer;
import java.util.Set;

/**
 * Central stat calculation service.
 */
public interface AttributeCalculationService {

    /**
     * Calculates a single final stat value.
     *
     * @param container source container
     * @param statId target stat id
     * @return calculated final value
     */
    double calculate(AttributeContainer container, AssetId statId);

    /**
     * Resolves dependency-aware dependents for a stat.
     *
     * @param statId source stat id
     * @return immutable dependent set
     */
    Set<AssetId> dependentsOf(AssetId statId);
}
