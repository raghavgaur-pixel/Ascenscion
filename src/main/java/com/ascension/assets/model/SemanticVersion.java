package com.ascension.assets.model;

import java.util.Objects;

/**
 * Simple semantic version value object.
 *
 * @param major major version
 * @param minor minor version
 * @param patch patch version
 */
public record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {

    /**
     * Parses a semantic version string.
     *
     * @param raw version text
     * @return parsed version
     */
    public static SemanticVersion parse(final String raw) {
        final String[] parts = Objects.requireNonNull(raw, "raw").split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Semantic version must use major.minor.patch format: " + raw);
        }
        return new SemanticVersion(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }

    @Override
    public int compareTo(final SemanticVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.patch;
    }
}

