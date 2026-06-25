package com.ascension.assets.localization;

import com.ascension.assets.model.AssetId;
import com.ascension.assets.registry.AssetRegistry;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default translation service backed by translation bundle assets.
 */
public final class DefaultLocalizationService implements LocalizationService {

    private final Supplier<LocalizationSettings> settingsSupplier;
    private final AssetRegistry<TranslationBundleDefinition> bundles;

    public DefaultLocalizationService(
        final Supplier<LocalizationSettings> settingsSupplier,
        final AssetRegistry<TranslationBundleDefinition> bundles
    ) {
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
        this.bundles = Objects.requireNonNull(bundles, "bundles");
    }

    @Override
    public String translate(final Locale locale, final String key, final Map<String, Object> arguments) {
        final LocalizationSettings settings = this.settingsSupplier.get();
        final Map<String, Object> resolvedArguments = arguments == null ? Map.of() : arguments;
        final String requestedLocale = locale.toLanguageTag().replace('-', '_').toLowerCase(Locale.ROOT);
        String value = findMessage(requestedLocale, key);
        if (value == null) {
            value = findMessage(settings.bundleNamespace(), settings.fallbackLanguage(), key);
        }
        if (value == null) {
            value = key;
        }
        return format(value, resolvedArguments);
    }

    @Override
    public String pluralize(
        final Locale locale,
        final String key,
        final long quantity,
        final Map<String, Object> arguments
    ) {
        final String variant = quantity == 1L ? key + ".one" : key + ".other";
        final java.util.LinkedHashMap<String, Object> values =
            new java.util.LinkedHashMap<>(arguments == null ? Map.of() : arguments);
        values.putIfAbsent("count", quantity);
        return this.translate(locale, variant, values);
    }

    private String findMessage(final String localeTag, final String key) {
        return findMessage(this.settingsSupplier.get().bundleNamespace(), localeTag, key);
    }

    private String findMessage(final String namespace, final String localeTag, final String key) {
        final AssetId assetId = new AssetId(namespace, localeTag);
        return this.bundles.find(assetId)
            .map(bundle -> bundle.messages().get(key))
            .orElse(null);
    }

    private static String format(final String message, final Map<String, Object> arguments) {
        String formatted = message;
        for (final Map.Entry<String, Object> entry : arguments.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", Objects.toString(entry.getValue()));
        }
        return formatted;
    }
}
