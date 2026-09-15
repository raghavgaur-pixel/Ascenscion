package com.ascension.serialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    public SerializedObject without(final String key) {
        final Map<String, Object> copied = new LinkedHashMap<>(this.values);
        copied.remove(Objects.requireNonNull(key, "key"));
        return SerializedObject.copyOf(copied);
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

    public double getDouble(final String key, final double defaultValue) {
        final Object value = this.values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            return Double.parseDouble(stringValue);
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

    /**
     * Returns a nested object when the stored value is a map-like configuration node.
     */
    public Optional<SerializedObject> getObject(final String key) {
        final Object value = this.values.get(key);
        if (value instanceof Map<?, ?> mapValue) {
            final Map<String, Object> normalized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return Optional.of(SerializedObject.copyOf(normalized));
        }
        if (value instanceof ConfigurationSection section) {
            return Optional.of(SerializedObject.copyOf(section.getValues(false)));
        }
        return Optional.empty();
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

    public Map<String, Double> getDoubleMap(final String key) {
        final Object value = this.values.get(key);
        final Map<?, ?> rawMap;
        if (value instanceof Map<?, ?> mapValue) {
            rawMap = mapValue;
        } else if (value instanceof ConfigurationSection section) {
            rawMap = section.getValues(false);
        } else {
            return Map.of();
        }

        final Map<String, Double> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            final Object entryValue = entry.getValue();
            if (entryValue instanceof Number number) {
                result.put(entry.getKey().toString(), number.doubleValue());
            } else if (entryValue instanceof String stringValue) {
                result.put(entry.getKey().toString(), Double.parseDouble(stringValue));
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

    public java.util.List<SerializedObject> getObjectList(final String key) {
        final Object value = this.values.get(key);
        if (!(value instanceof Collection<?> collection)) {
            return java.util.List.of();
        }

        final java.util.List<SerializedObject> result = new java.util.ArrayList<>();
        for (final Object entry : collection) {
            if (entry instanceof Map<?, ?> map) {
                final Map<String, Object> stringMap = new LinkedHashMap<>();
                for (final Map.Entry<?, ?> mapEntry : map.entrySet()) {
                    if (mapEntry.getKey() != null) {
                        stringMap.put(mapEntry.getKey().toString(), mapEntry.getValue());
                    }
                }
                result.add(SerializedObject.copyOf(stringMap));
            } else if (entry instanceof ConfigurationSection section) {
                result.add(SerializedObject.copyOf(section.getValues(false)));
            }
        }
        return java.util.List.copyOf(result);
    }

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

    private static Object mergeValue(final Object baseValue, final Object overrideValue) {
        final Object normalizedBase = baseValue instanceof ConfigurationSection baseSection
            ? baseSection.getValues(false)
            : baseValue;
        final Object normalizedOverride = overrideValue instanceof ConfigurationSection overrideSection
            ? overrideSection.getValues(false)
            : overrideValue;

        if (normalizedBase instanceof Map<?, ?> baseMap && normalizedOverride instanceof Map<?, ?> overrideMap) {
            final Map<String, Object> merged = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : baseMap.entrySet()) {
                if (entry.getKey() != null) {
                    merged.put(entry.getKey().toString(), entry.getValue());
                }
            }
            for (final Map.Entry<?, ?> entry : overrideMap.entrySet()) {
                if (entry.getKey() != null) {
                    final String key = entry.getKey().toString();
                    merged.put(key, mergeValue(merged.get(key), entry.getValue()));
                }
            }
            return merged;
        }
        return normalizedOverride;
    }
}
