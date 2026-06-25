package com.ascension.assets.loader;

import com.ascension.serialization.SerializedFormat;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Source metadata for a discovered asset file.
 *
 * @param path source path
 * @param format serialized format
 */
public record AssetSource(Path path, SerializedFormat format) {

    public AssetSource {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(format, "format");
    }
}

