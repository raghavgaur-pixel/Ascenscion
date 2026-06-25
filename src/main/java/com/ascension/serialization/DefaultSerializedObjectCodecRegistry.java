package com.ascension.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory codec registry.
 */
public final class DefaultSerializedObjectCodecRegistry implements SerializedObjectCodecRegistry {

    private final Map<SerializedFormat, SerializedObjectCodec> codecs = new ConcurrentHashMap<>();

    @Override
    public void register(final SerializedObjectCodec codec) {
        final SerializedObjectCodec previous = this.codecs.putIfAbsent(codec.format(), codec);
        if (previous != null) {
            throw new IllegalStateException("Codec already registered for format: " + codec.format());
        }
    }

    @Override
    public SerializedObjectCodec require(final SerializedFormat format) {
        final SerializedObjectCodec codec = this.codecs.get(format);
        if (codec == null) {
            throw new IllegalStateException("No codec registered for format: " + format);
        }
        return codec;
    }
}

