package com.ascension.stats.modifier;

import java.util.Objects;

/**
 * Stable source descriptor for audit, replacement, and cleanup logic.
 *
 * @param owner owning subsystem or module
 * @param type source type
 * @param id source identifier
 */
public record ModifierSource(
    String owner,
    String type,
    String id
) {

    public ModifierSource {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }
}
