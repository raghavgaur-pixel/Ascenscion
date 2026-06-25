package com.ascension.assets.loader;

import com.ascension.core.config.typed.TypedConfigurationCodec;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.serialization.SerializedObject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec for asset framework settings.
 */
public final class AssetFrameworkSettingsCodec implements TypedConfigurationCodec<AssetFrameworkSettings> {

    @Override
    public AssetFrameworkSettings decode(final SerializedObject object) {
        return new AssetFrameworkSettings(
            object.getString("root_directory", "assets"),
            object.getString("default_namespace", "ascension"),
            SemanticVersion.parse(object.getString("engine_version", "1.0.0")),
            object.getBoolean("strict_validation", true),
            object.getStringMap("owned_directories")
        );
    }

    @Override
    public SerializedObject encode(final AssetFrameworkSettings value) {
        final Map<String, String> directories = new LinkedHashMap<>(value.ownedDirectories());
        return SerializedObject.builder()
            .put("root_directory", value.rootDirectory())
            .put("default_namespace", value.defaultNamespace())
            .put("engine_version", value.engineVersion().toString())
            .put("strict_validation", value.strictValidation())
            .put("owned_directories", directories)
            .build();
    }
}

