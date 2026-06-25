package com.ascension.assets.localization;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.serialization.SerializedObject;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable translation bundle definition.
 *
 * @param descriptor common descriptor
 * @param localeTag locale tag
 * @param messages translation messages
 * @param data raw payload
 */
public record TranslationBundleDefinition(
    AssetDescriptor descriptor,
    String localeTag,
    Map<String, String> messages,
    SerializedObject data
) implements AssetDefinition {

    public TranslationBundleDefinition {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(localeTag, "localeTag");
        messages = Map.copyOf(messages);
        Objects.requireNonNull(data, "data");
    }

    @Override
    public String type() {
        return "localization";
    }
}
