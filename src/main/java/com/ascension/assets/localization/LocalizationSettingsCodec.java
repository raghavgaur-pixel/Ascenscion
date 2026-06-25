package com.ascension.assets.localization;

import com.ascension.core.config.typed.TypedConfigurationCodec;
import com.ascension.serialization.SerializedObject;

/**
 * Codec for localization settings.
 */
public final class LocalizationSettingsCodec implements TypedConfigurationCodec<LocalizationSettings> {

    @Override
    public LocalizationSettings decode(final SerializedObject object) {
        return new LocalizationSettings(
            object.getString("default_language", "en_us"),
            object.getString("fallback_language", "en_us"),
            object.getString("bundle_namespace", "ascension")
        );
    }

    @Override
    public SerializedObject encode(final LocalizationSettings value) {
        return SerializedObject.builder()
            .put("default_language", value.defaultLanguage())
            .put("fallback_language", value.fallbackLanguage())
            .put("bundle_namespace", value.bundleNamespace())
            .build();
    }
}
