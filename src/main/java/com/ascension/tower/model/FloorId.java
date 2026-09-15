package com.ascension.tower.model;

import java.util.Objects;

/**
 * Stable application-level identifier for a tower floor.
 */
public record FloorId(String value) {

    public FloorId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("floor id cannot be blank");
        }
    }

    public static FloorId of(final String value) {
        return new FloorId(value);
    }

    @Override
    public String toString() {
        return this.value;
    }
}
