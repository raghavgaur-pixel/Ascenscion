package com.ascension.assets.loader;

import com.ascension.assets.localization.TranslationBundleDefinition;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.serialization.SerializedObject;
import java.util.Map;
import java.util.Set;

/**
 * Serializer for translation bundle assets.
 */
public final class TranslationBundleSerializer implements AssetSerializer<TranslationBundleDefinition> {

    @Override
    public TranslationBundleDefinition deserialize(
        final AssetSource source,
        final SerializedObject object,
        final String owner
    ) {
        final String localeTag = object.getString("locale", "");
        return new TranslationBundleDefinition(
            new AssetDescriptor(
                AssetId.parse(object.getString("id", "")),
                owner,
                SemanticVersion.parse(object.getString("version", "1.0.0")),
                object.getString("display_name", localeTag),
                object.getString("description", ""),
                object.getStringMap("metadata"),
                AssetCompatibility.open(),
                Set.of()
            ),
            localeTag,
            object.getStringMap("messages"),
            SerializedObject.builder().put("messages", object.getStringMap("messages")).build()
        );
    }

    @Override
    public SerializedObject serialize(final TranslationBundleDefinition definition) {
        return SerializedObject.builder()
            .put("id", definition.id().toString())
            .put("version", definition.version().toString())
            .put("locale", definition.localeTag())
            .put("display_name", definition.displayName())
            .put("description", definition.description())
            .put("messages", definition.messages())
            .put("metadata", definition.metadata())
            .build();
    }
}

