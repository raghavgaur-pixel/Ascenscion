package com.ascension.assets.localization;

import java.util.Objects;

/**
 * Localization framework settings.
 *
 * @param defaultLanguage default language tag
 * @param fallbackLanguage fallback language tag
 * @param bundleNamespace namespace used for translation bundle lookup
 */
public record LocalizationSettings(
    String defaultLanguage,
    String fallbackLanguage,
    String bundleNamespace
) {

    public LocalizationSettings {
        Objects.requireNonNull(defaultLanguage, "defaultLanguage");
        Objects.requireNonNull(fallbackLanguage, "fallbackLanguage");
        Objects.requireNonNull(bundleNamespace, "bundleNamespace");
    }
}
