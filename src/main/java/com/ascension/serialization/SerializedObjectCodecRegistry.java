package com.ascension.serialization;

/**
 * Registry for structured object codecs.
 */
public interface SerializedObjectCodecRegistry {

    /**
     * Registers a codec.
     *
     * @param codec codec to register
     */
    void register(SerializedObjectCodec codec);

    /**
     * Resolves a codec by format.
     *
     * @param format desired format
     * @return matching codec
     */
    SerializedObjectCodec require(SerializedFormat format);
}

