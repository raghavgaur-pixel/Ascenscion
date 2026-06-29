package com.ascension.effects.definition;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.effects.model.EffectDurationType;
import com.ascension.effects.model.EffectStackingBehavior;
import com.ascension.serialization.SerializedObject;
import com.ascension.stats.modifier.ModifierCategory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable blueprint for a gameplay effect.
 *
 * @param descriptor asset metadata
 * @param category effect modifier category (e.g. BUFF, DEBUFF)
 * @param durationType duration management policy
 * @param baseDuration optional base duration if TIMED
 * @param stackingBehavior stacking policy
 * @param maxStacks max stacks if applicable
 * @param modifiers stat modifiers applied by this effect
 */
public record EffectDefinition(
    AssetDescriptor descriptor,
    ModifierCategory category,
    EffectDurationType durationType,
    Optional<Duration> baseDuration,
    EffectStackingBehavior stackingBehavior,
    int maxStacks,
    List<EffectModifierDefinition> modifiers
) implements AssetDefinition {

    public EffectDefinition {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(durationType, "durationType");
        Objects.requireNonNull(baseDuration, "baseDuration");
        Objects.requireNonNull(stackingBehavior, "stackingBehavior");
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    }

    @Override
    public SerializedObject data() {
        return SerializedObject.empty();
    }

    @Override
    public String type() {
        return "effect";
    }
}
