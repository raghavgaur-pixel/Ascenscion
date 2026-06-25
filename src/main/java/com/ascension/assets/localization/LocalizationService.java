package com.ascension.assets.localization;

import java.util.Locale;
import java.util.Map;

/**
 * Localization service with fallback behavior and simple message formatting.
 */
public interface LocalizationService {

    /**
     * Resolves a translated message.
     *
     * @param locale requested locale
     * @param key translation key
     * @param arguments named arguments
     * @return translated text or key fallback
     */
    String translate(Locale locale, String key, Map<String, Object> arguments);

    /**
     * Resolves a pluralized message.
     *
     * @param locale requested locale
     * @param key translation base key
     * @param quantity quantity for pluralization
     * @param arguments named arguments
     * @return translated text
     */
    String pluralize(Locale locale, String key, long quantity, Map<String, Object> arguments);
}

