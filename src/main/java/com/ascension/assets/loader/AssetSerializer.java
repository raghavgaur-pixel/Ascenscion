package com.ascension.assets.loader;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.serialization.SerializedObject;

/**
 * Deserializes and serializes asset definitions.
 *
 * @param <T> asset type
 */
public interface AssetSerializer<T extends AssetDefinition> {

    /**
     * Reads an asset definition.
     *
     * @param source asset source metadata
     * @param object raw structured data
     * @param owner asset owner
     * @return deserialized asset
     */
    T deserialize(AssetSource source, SerializedObject object, String owner);

    /**
     * Writes an asset definition.
     *
     * @param definition asset definition
     * @return serialized data
     */
    SerializedObject serialize(T definition);
}

