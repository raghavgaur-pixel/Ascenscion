package com.ascension.serialization;

/**
 * Codec for converting {@link SerializedObject} instances to and from a named format.
 */
public interface SerializedObjectCodec {

    /**
     * @return supported format
     */
    SerializedFormat format();

    /**
     * Encodes a structured object.
     *
     * @param object object to encode
     * @return serialized payload
     */
    String encode(SerializedObject object);

    /**
     * Decodes a structured object.
     *
     * @param payload serialized payload
     * @return structured object
     */
    SerializedObject decode(String payload);
}

