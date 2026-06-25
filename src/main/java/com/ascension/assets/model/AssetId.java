package com.ascension.assets.model;

import java.util.Objects;

/**
 * Stable asset identifier in {@code namespace:value} form.
 *
 * @param namespace asset namespace
 * @param value asset value
 */
public record AssetId(String namespace, String value) {

    public AssetId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(value, "value");
    }

    /**
     * Parses an asset id.
     *
     * @param raw raw text
     * @return parsed asset id
     */
    public static AssetId parse(final String raw) {
        final String[] parts = Objects.requireNonNull(raw, "raw").split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Asset id must use namespace:value format: " + raw);
        }
        return new AssetId(parts[0], parts[1]);
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.value;
    }
}

