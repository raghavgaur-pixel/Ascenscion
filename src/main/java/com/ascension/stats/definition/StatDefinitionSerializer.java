package com.ascension.stats.definition;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Serializer for stat definition assets.
 */
public final class StatDefinitionSerializer implements AssetSerializer<StatDefinition> {

    @Override
    public StatDefinition deserialize(
        final AssetSource source,
        final SerializedObject object,
        final String owner
    ) {
        final Map<String, Object> formulaMap = asMap(object.asMap().get("formula"));
        final Optional<DerivedFormulaDefinition> formula = formulaMap.isEmpty()
            ? Optional.empty()
            : Optional.of(parseFormula(formulaMap));

        final Set<AssetReference> dependencies = new LinkedHashSet<>();
        final Object dependsOnValue = object.asMap().get("depends_on");
        if (dependsOnValue instanceof Iterable<?> iterable) {
            for (final Object entry : iterable) {
                if (entry != null) {
                    dependencies.add(new AssetReference("stats", AssetId.parse(entry.toString())));
                }
            }
        }
        formula.ifPresent(value -> value.coefficients().keySet()
            .forEach(id -> dependencies.add(new AssetReference("stats", id))));

        return new StatDefinition(
            new AssetDescriptor(
                AssetId.parse(object.getString("id", "")),
                owner,
                SemanticVersion.parse(object.getString("version", "1.0.0")),
                object.getString("display_name", ""),
                object.getString("description", ""),
                object.getStringMap("metadata"),
                parseCompatibility(object),
                dependencies
            ),
            StatCategory.valueOf(object.getString("category", "UTILITY").toUpperCase(java.util.Locale.ROOT)),
            parseDouble(object.asMap().get("base_value"), 0.0D),
            parseOptionalDouble(object.asMap().get("minimum_value")),
            parseOptionalDouble(object.asMap().get("maximum_value")),
            (int) parseDouble(object.asMap().get("decimal_places"), 2.0D),
            formula,
            object
        );
    }

    @Override
    public SerializedObject serialize(final StatDefinition definition) {
        final SerializedObject.Builder builder = SerializedObject.builder()
            .put("id", definition.id().toString())
            .put("version", definition.version().toString())
            .put("display_name", definition.displayName())
            .put("description", definition.description())
            .put("metadata", definition.metadata())
            .put("category", definition.category().name().toLowerCase(java.util.Locale.ROOT))
            .put("base_value", definition.defaultBaseValue())
            .put("decimal_places", definition.decimalPlaces());

        definition.minimumValue().ifPresent(value -> builder.put("minimum_value", value));
        definition.maximumValue().ifPresent(value -> builder.put("maximum_value", value));
        definition.formula().ifPresent(formula -> builder.put("formula", serializeFormula(formula)));

        final java.util.List<String> dependencies = definition.dependencies().stream()
            .map(reference -> reference.id().toString())
            .toList();
        if (!dependencies.isEmpty()) {
            builder.put("depends_on", dependencies);
        }
        return builder.build();
    }

    private static DerivedFormulaDefinition parseFormula(final Map<String, Object> formulaMap) {
        final Map<String, Object> coefficientsMap = asMap(formulaMap.get("coefficients"));
        final Map<AssetId, Double> coefficients = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : coefficientsMap.entrySet()) {
            coefficients.put(AssetId.parse(entry.getKey()), parseDouble(entry.getValue(), 0.0D));
        }
        return new DerivedFormulaDefinition(
            Objects.requireNonNullElse(formulaMap.get("type"), "linear").toString(),
            parseDouble(formulaMap.get("base"), 0.0D),
            coefficients
        );
    }

    private static AssetCompatibility parseCompatibility(final SerializedObject object) {
        final Map<String, Object> compatibilityMap = asMap(object.asMap().get("compatibility"));
        if (compatibilityMap.isEmpty()) {
            return AssetCompatibility.open();
        }
        return new AssetCompatibility(
            compatibilityMap.containsKey("min_engine_version")
                ? Optional.of(SemanticVersion.parse(compatibilityMap.get("min_engine_version").toString()))
                : Optional.empty(),
            compatibilityMap.containsKey("max_engine_version")
                ? Optional.of(SemanticVersion.parse(compatibilityMap.get("max_engine_version").toString()))
                : Optional.empty()
        );
    }

    private static Map<String, Object> serializeFormula(final DerivedFormulaDefinition formula) {
        final Map<String, Object> coefficients = new LinkedHashMap<>();
        for (final Map.Entry<AssetId, Double> entry : formula.coefficients().entrySet()) {
            coefficients.put(entry.getKey().toString(), entry.getValue());
        }
        final Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("type", formula.type());
        serialized.put("base", formula.baseValue());
        serialized.put("coefficients", coefficients);
        return serialized;
    }

    private static Optional<Double> parseOptionalDouble(final Object value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(parseDouble(value, 0.0D));
    }

    private static double parseDouble(final Object value, final double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Double.parseDouble(stringValue);
        }
        return defaultValue;
    }

    private static Map<String, Object> asMap(final Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        final Map<String, Object> result = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }
}
