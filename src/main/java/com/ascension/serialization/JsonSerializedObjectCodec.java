package com.ascension.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-backed codec for structured objects.
 */
public final class JsonSerializedObjectCodec implements SerializedObjectCodec {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonSerializedObjectCodec(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SerializedFormat format() {
        return SerializedFormat.JSON;
    }

    @Override
    public String encode(final SerializedObject object) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object.asMap());
        } catch (final JsonProcessingException exception) {
            throw new IllegalStateException("Failed to encode JSON payload.", exception);
        }
    }

    @Override
    public SerializedObject decode(final String payload) {
        try {
            final Map<String, Object> values = this.objectMapper.readValue(
                payload == null ? "{}" : payload,
                MAP_TYPE
            );
            return SerializedObject.copyOf(new LinkedHashMap<>(values));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to decode JSON payload.", exception);
        }
    }
}

