package com.ascension.assets.loader;

import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.AssetReference;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.serialization.SerializedObject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Generic serializer for asset definitions that share the standard asset document shape.
 *
 * @param <T> asset type
 */
public final class GenericAssetSerializer<T extends AssetDefinition> implements AssetSerializer<T> {

    private final String assetType;
    private final BiFunction<AssetDescriptor, SerializedObject, T> factory;

    public GenericAssetSerializer(
        final String assetType,
        final BiFunction<AssetDescriptor, SerializedObject, T> factory
    ) {
        this.assetType = assetType;
        this.factory = factory;
    }

    @Override
    public T deserialize(final AssetSource source, final SerializedObject object, final String owner) {
        final Set<AssetReference> dependencies = new LinkedHashSet<>();
        final Object dependenciesValue = object.asMap().get("depends_on");
        if (dependenciesValue instanceof Iterable<?> iterable) {
            for (final Object entry : iterable) {
                if (entry instanceof Map<?, ?> dependencyMap) {
                    final Object type = dependencyMap.get("type");
                    final Object id = dependencyMap.get("id");
                    if (type != null && id != null) {
                        dependencies.add(new AssetReference(type.toString(), AssetId.parse(id.toString())));
                    }
                } else if (entry != null) {
                    dependencies.add(new AssetReference(this.assetType, AssetId.parse(entry.toString())));
                }
            }
        }

        final Map<String, Object> compatibilityMap = asMap(object.asMap().get("compatibility"));
        final AssetCompatibility compatibility = compatibilityMap.isEmpty()
            ? AssetCompatibility.open()
            : new AssetCompatibility(
                compatibilityMap.containsKey("min_engine_version")
                    ? java.util.Optional.of(SemanticVersion.parse(compatibilityMap.get("min_engine_version").toString()))
                    : java.util.Optional.empty(),
                compatibilityMap.containsKey("max_engine_version")
                    ? java.util.Optional.of(SemanticVersion.parse(compatibilityMap.get("max_engine_version").toString()))
                    : java.util.Optional.empty()
            );

        final Map<String, Object> dataMap = asMap(object.asMap().getOrDefault("data", Map.of()));
        return this.factory.apply(
            new AssetDescriptor(
                AssetId.parse(object.getString("id", "")),
                owner,
                SemanticVersion.parse(object.getString("version", "1.0.0")),
                object.getString("display_name", ""),
                object.getString("description", ""),
                object.getStringMap("metadata"),
                compatibility,
                dependencies
            ),
            SerializedObject.copyOf(dataMap)
        );
    }

    @Override
    public SerializedObject serialize(final T definition) {
        final java.util.List<Map<String, String>> dependencies = definition.dependencies().stream()
            .map(reference -> Map.of(
                "type", reference.type(),
                "id", reference.id().toString()
            ))
            .toList();
        final java.util.LinkedHashMap<String, Object> compatibility = new java.util.LinkedHashMap<>();
        definition.compatibility().minimumEngineVersion()
            .ifPresent(version -> compatibility.put("min_engine_version", version.toString()));
        definition.compatibility().maximumEngineVersion()
            .ifPresent(version -> compatibility.put("max_engine_version", version.toString()));
        return SerializedObject.builder()
            .put("id", definition.id().toString())
            .put("version", definition.version().toString())
            .put("display_name", definition.displayName())
            .put("description", definition.description())
            .put("metadata", definition.metadata())
            .put("depends_on", dependencies)
            .put("compatibility", compatibility)
            .put("data", definition.data().asMap())
            .build();
    }

    private static Map<String, Object> asMap(final Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            final java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return result;
        }
        return Map.of();
    }
}
