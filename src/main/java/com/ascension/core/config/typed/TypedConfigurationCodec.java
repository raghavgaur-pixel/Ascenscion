package com.ascension.core.config.typed;

import com.ascension.serialization.SerializedObject;

/**
 * Codec for typed configuration values.
 *
 * @param <T> configuration type
 */
public interface TypedConfigurationCodec<T> {

    /**
     * Decodes a configuration value.
     *
     * @param object raw structured config
     * @return typed value
     */
    T decode(SerializedObject object);

    /**
     * Encodes a configuration value.
     *
     * @param value typed value
     * @return raw structured config
     */
    SerializedObject encode(T value);
}

