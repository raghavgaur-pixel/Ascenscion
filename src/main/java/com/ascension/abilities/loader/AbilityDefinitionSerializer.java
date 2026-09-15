package com.ascension.abilities.loader;

import com.ascension.abilities.model.AbilityCost;
import com.ascension.abilities.model.AbilityTargetType;
import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.loader.AssetSerializer;
import com.ascension.assets.loader.AssetSource;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.AssetReference;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.serialization.SerializedObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Serializer for the first-class ability asset schema.
 */
public final class AbilityDefinitionSerializer implements AssetSerializer<AbilityDefinition> {

    @Override
    public AbilityDefinition deserialize(final AssetSource source, final SerializedObject object, final String owner) {
        final Set<AssetReference> dependencies = new LinkedHashSet<>();
        final Object dependencyValue = object.asMap().get("depends_on");
        if (dependencyValue instanceof Iterable<?> iterable) {
            for (final Object entry : iterable) {
                if (entry instanceof Map<?, ?> dependencyMap) {
                    final Object type = dependencyMap.get("type");
                    final Object id = dependencyMap.get("id");
                    if (type != null && id != null) {
                        dependencies.add(new AssetReference(type.toString(), AssetId.parse(id.toString())));
                    }
                } else if (entry != null) {
                    dependencies.add(new AssetReference("abilities", AssetId.parse(entry.toString())));
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

        final Map<String, Object> data = asMap(object.asMap().get("data"));
        return new AbilityDefinition(
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
            parseTarget(object.getString("target", "NONE")),
            new AbilityCost(object.getDoubleMap("cost")),
            object.getLong("cooldown_millis", 0L),
            SerializedObject.copyOf(data)
        );
    }

    @Override
    public SerializedObject serialize(final AbilityDefinition definition) {
        final var dependencies = definition.dependencies().stream()
            .map(reference -> Map.of("type", reference.type(), "id", reference.id().toString()))
            .toList();
        final var compatibility = new LinkedHashMap<String, Object>();
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
            .put("target", definition.targetType().name())
            .put("cost", definition.cost().resources())
            .put("cooldown_millis", definition.cooldownMillis())
            .put("data", definition.data().asMap())
            .build();
    }

    private static AbilityTargetType parseTarget(final String raw) {
        try {
            return AbilityTargetType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown ability target type: " + raw, exception);
        }
    }

    private static Map<String, Object> asMap(final Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return result;
        }
        if (value instanceof org.bukkit.configuration.ConfigurationSection section) {
            return new LinkedHashMap<>(section.getValues(false));
        }
        return Map.of();
    }
}
