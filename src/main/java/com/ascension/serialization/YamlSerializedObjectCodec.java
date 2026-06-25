package com.ascension.serialization;

import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * YAML-backed codec used for flexible structured payload persistence.
 */
public final class YamlSerializedObjectCodec {

    /**
     * Encodes a structured object to YAML text.
     *
     * @param object structured data
     * @return yaml payload
     */
    public String encode(final SerializedObject object) {
        final YamlConfiguration configuration = new YamlConfiguration();
        for (final Map.Entry<String, Object> entry : object.asMap().entrySet()) {
            configuration.set(entry.getKey(), entry.getValue());
        }
        return configuration.saveToString();
    }

    /**
     * Decodes a YAML payload to structured data.
     *
     * @param yaml yaml payload
     * @return structured data
     */
    public SerializedObject decode(final String yaml) {
        final YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml == null ? "" : yaml);
        } catch (final InvalidConfigurationException exception) {
            throw new IllegalStateException("Invalid YAML payload encountered during decoding.", exception);
        }

        final Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (final String key : configuration.getKeys(false)) {
            values.put(key, configuration.get(key));
        }
        return SerializedObject.copyOf(values);
    }
}
