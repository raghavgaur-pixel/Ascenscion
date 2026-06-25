package com.ascension.stats.service;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.calculation.DerivedStatCalculator;
import com.ascension.stats.definition.StatDefinition;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only stat registry facade with derived calculator resolution.
 */
public interface StatService {

    /**
     * Resolves a stat definition.
     *
     * @param statId stat id
     * @return definition if present
     */
    Optional<StatDefinition> find(AssetId statId);

    /**
     * Resolves a stat definition or fails.
     *
     * @param statId stat id
     * @return definition
     */
    StatDefinition require(AssetId statId);

    /**
     * Resolves a derived calculator if the stat is derived.
     *
     * @param statId stat id
     * @return calculator if present
     */
    Optional<DerivedStatCalculator> calculator(AssetId statId);

    /**
     * @return immutable stat snapshot
     */
    Collection<StatDefinition> definitions();

    /**
     * @return immutable stat key snapshot
     */
    Set<AssetId> statIds();
}
