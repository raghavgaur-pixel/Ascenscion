package com.ascension.serialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Structured key-value container used for persistence-safe serialization.
 */
public final class SerializedObject {

    private final Map<String, Object> values;

    private SerializedObject(final Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static SerializedObject empty() {
        return new SerializedObject(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SerializedObject copyOf(final Map<String, Object> values) {
        return new SerializedObject(new LinkedHashMap<>(values));
    }

    public Map<String, Object> asMap() {
        return this.values;
    }

    public String getString(final String key, final String defaultValue) {
        final Object value = this.values.get(key);
        return value instanceof String stringValue ? stringValue : defaultValue;
    }

    public long getLong(final String key, final long defaultValue) {
        final Object value = this.values.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        return defaultValue;
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        final Object value = this.values.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return defaultValue;
    }

    public Map<String, Long> getLongMap(final String key) {
        final Object value = this.values.get(key);
        final Map<?, ?> rawMap;
        if (value instanceof Map<?, ?> mapValue) {
            rawMap = mapValue;
        } else if (value instanceof ConfigurationSection section) {
            rawMap = section.getValues(false);
        } else {
            return Map.of();
        }

        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            final Object entryValue = entry.getValue();
            if (entryValue instanceof Number number) {
                result.put(entry.getKey().toString(), number.longValue());
            } else if (entryValue instanceof String stringValue) {
                result.put(entry.getKey().toString(), Long.parseLong(stringValue));
            }
        }
        return Map.copyOf(result);
    }

    public Map<String, String> getStringMap(final String key) {
        final Object value = this.values.get(key);
        final Map<?, ?> rawMap;
        if (value instanceof Map<?, ?> mapValue) {
            rawMap = mapValue;
        } else if (value instanceof ConfigurationSection section) {
            rawMap = section.getValues(false);
        } else {
            return Map.of();
        }

        final Map<String, String> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return Map.copyOf(result);
    }

    public Set<String> getStringSet(final String key) {
        final Object value = this.values.get(key);
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }

        final java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (final Object entry : collection) {
            if (entry != null) {
                result.add(entry.toString());
            }
        }
        return Set.copyOf(result);
    }

    /**
     * Mutable builder for a serialized object.
     */
    public static final class Builder {

        private final Map<String, Object> values = new LinkedHashMap<>();

        public Builder put(final String key, final Object value) {
            this.values.put(Objects.requireNonNull(key, "key"), value);
            return this;
        }

        public SerializedObject build() {
            return new SerializedObject(this.values);
        }
    }
}
