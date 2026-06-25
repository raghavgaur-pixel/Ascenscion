package com.ascension.serialization;

/**
 * Contract for converting rich domain objects into structured persistence data and back.
 *
 * @param <T> object type
 */
public interface ObjectCodec<T> {

    /**
     * Serializes an object into a structured form.
     *
     * @param value source value
     * @return serialized structure
     */
    SerializedObject encode(T value);

    /**
     * Deserializes a value from a structured form.
     *
     * @param value serialized structure
     * @return decoded object
     */
    T decode(SerializedObject value);
}

