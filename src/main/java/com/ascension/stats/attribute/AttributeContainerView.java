package com.ascension.stats.attribute;

import com.ascension.assets.model.AssetId;
import com.ascension.stats.definition.StatDefinition;
import java.util.Collection;
import java.util.Optional;

/**
 * Read-only runtime attribute view.
 */
public interface AttributeContainerView {

    /**
     * Looks up a stat definition.
     *
     * @param statId stat id
     * @return stat definition if present
     */
    Optional<StatDefinition> stat(AssetId statId);

    /**
     * Resolves a final stat value.
     *
     * @param statId stat id
     * @return resolved value
     */
    double finalValue(AssetId statId);

    /**
     * Resolves a base stat value.
     *
     * @param statId stat id
     * @return base value
     */
    double baseValue(AssetId statId);

    /**
     * @return immutable stat key snapshot
     */
    Collection<AssetId> statIds();

    /**
     * @return immutable container snapshot
     */
    AttributeSnapshot snapshot();
}
