package com.ascension.effects.definition;

import com.ascension.assets.loader.AssetSerializer;
import com.ascension.assets.loader.AssetSource;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.effects.model.EffectDurationType;
import com.ascension.effects.model.EffectStackingBehavior;
import com.ascension.serialization.SerializedObject;
import com.ascension.stats.modifier.ModifierCategory;
import com.ascension.stats.modifier.ModifierOperation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EffectDefinitionSerializer implements AssetSerializer<EffectDefinition> {

    @Override
    public EffectDefinition deserialize(AssetSource source, SerializedObject object, String owner) {
        final AssetDescriptor descriptor = new AssetDescriptor(
            AssetId.parse(object.getString("id", "")),
            owner,
            SemanticVersion.parse(object.getString("version", "1.0.0")),
            object.getString("display_name", ""),
            object.getString("description", ""),
            object.getStringMap("metadata"),
            AssetCompatibility.open(),
            java.util.Set.of()
        );

        final ModifierCategory category = ModifierCategory.valueOf(object.getString("category", "BUFF").toUpperCase());
        final EffectDurationType durationType = EffectDurationType.valueOf(object.getString("duration_type", "TIMED").toUpperCase());

        Optional<Duration> baseDuration = Optional.empty();
        long baseDurationMs = object.getLong("base_duration_ms", -1);
        if (baseDurationMs != -1) {
            baseDuration = Optional.of(Duration.ofMillis(baseDurationMs));
        }

        final EffectStackingBehavior stackingBehavior = EffectStackingBehavior.valueOf(
            object.getString("stacking_behavior", "REPLACE_EXISTING").toUpperCase()
        );
        final int maxStacks = (int) object.getLong("max_stacks", 1);

        final List<EffectModifierDefinition> modifiers = new ArrayList<>();
        for (final SerializedObject modObj : object.getObjectList("modifiers")) {
            final AssetId statId = AssetId.parse(modObj.getString("stat", ""));
            final ModifierOperation operation = ModifierOperation.valueOf(modObj.getString("operation", "FLAT").toUpperCase());
            final double value = modObj.getDouble("value", 0.0);
            modifiers.add(new EffectModifierDefinition(statId, operation, value));
        }

        return new EffectDefinition(
            descriptor,
            category,
            durationType,
            baseDuration,
            stackingBehavior,
            maxStacks,
            modifiers
        );
    }

    @Override
    public SerializedObject serialize(EffectDefinition definition) {
        throw new UnsupportedOperationException("Serialization not supported for asset definitions");
    }
}
