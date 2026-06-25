package com.ascension.stats.attribute;

import com.ascension.assets.model.AssetId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a runtime attribute container.
 *
 * @param values resolved values keyed by stat id
 * @param revision container revision at snapshot time
 */
public record AttributeSnapshot(
    Map<AssetId, AttributeValueSnapshot> values,
    long revision
) {

    public AttributeSnapshot {
        values = Map.copyOf(values);
    }

    /**
     * Reads a stat snapshot.
     *
     * @param statId stat id
     * @return value if present
     */
    public Optional<AttributeValueSnapshot> find(final AssetId statId) {
        return Optional.ofNullable(this.values.get(Objects.requireNonNull(statId, "statId")));
    }
}
