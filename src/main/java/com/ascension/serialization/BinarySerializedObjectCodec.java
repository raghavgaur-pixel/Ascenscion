package com.ascension.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base64-wrapped binary-safe codec backed by JSON bytes.
 */
public final class BinarySerializedObjectCodec implements SerializedObjectCodec {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public BinarySerializedObjectCodec(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SerializedFormat format() {
        return SerializedFormat.BINARY;
    }

    @Override
    public String encode(final SerializedObject object) {
        try {
            final byte[] bytes = this.objectMapper.writeValueAsBytes(object.asMap());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to encode binary payload.", exception);
        }
    }

    @Override
    public SerializedObject decode(final String payload) {
        try {
            final byte[] bytes = payload == null || payload.isBlank()
                ? "{}".getBytes(StandardCharsets.UTF_8)
                : Base64.getDecoder().decode(payload);
            final Map<String, Object> values = this.objectMapper.readValue(bytes, MAP_TYPE);
            return SerializedObject.copyOf(new LinkedHashMap<>(values));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to decode binary payload.", exception);
        }
    }
}
