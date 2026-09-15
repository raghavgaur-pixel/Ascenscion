package com.ascension.abilities.service;

import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.abilities.model.AbilityRequest;
import com.ascension.combat.model.AttackContext;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.CombatTarget;
import com.ascension.combat.model.DamageContext;
import com.ascension.combat.model.DamageSource;
import com.ascension.combat.model.DefaultDamageType;
import com.ascension.combat.model.HitResult;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.serialization.SerializedObject;
import com.ascension.session.model.PlayerSession;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Executes the small set of generic gameplay behaviors used by authored
 * abilities. New content normally only needs YAML; bespoke mechanics can use
 * an explicit {@link AbilityExecutor} registration instead.
 */
public final class DataDrivenAbilityExecutor implements AbilityExecutor {

    private final AbilityRuntimeGateway runtime;

    public DataDrivenAbilityExecutor(final AbilityRuntimeGateway runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public AbilityExecutionResult execute(
        final AbilityRequest request,
        final AbilityDefinition definition
    ) {
        final String behavior = definition.data().getString("behavior", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (behavior.isBlank()) {
            return AbilityExecutionResult.rejected("Ability has no behavior");
        }

        return switch (behavior) {
            case "physical_attack", "area_physical_attack" -> executePhysicalAttack(request, definition);
            case "apply_effect" -> executeEffect(request, definition);
            default -> AbilityExecutionResult.rejected("Unsupported ability behavior: " + behavior);
        };
    }

    private AbilityExecutionResult executePhysicalAttack(
        final AbilityRequest request,
        final AbilityDefinition definition
    ) {
        if (request.targetId() == null) {
            return AbilityExecutionResult.rejected("Physical attack requires an entity target");
        }

        final Optional<CombatEntity> attacker = this.runtime.combatEntity(request.actorId());
        final Optional<CombatEntity> target = this.runtime.combatEntity(request.targetId());
        if (attacker.isEmpty()) {
            return AbilityExecutionResult.rejected("Actor is not a combat entity");
        }
        if (target.isEmpty()) {
            return AbilityExecutionResult.rejected("Target is not a combat entity");
        }
        if (!attacker.get().health().isAlive()) {
            return AbilityExecutionResult.rejected("Actor is not alive");
        }
        if (!target.get().health().isAlive()) {
            return AbilityExecutionResult.rejected("Target is not alive");
        }

        final double damage = calculateDamage(attacker.get(), definition.data());
        if (!Double.isFinite(damage) || damage < 0.0D) {
            return AbilityExecutionResult.rejected("Ability produced invalid damage");
        }

        final AssetId abilityId = definition.id();
        final HitResult result = this.runtime.attack(attacker.get(), target.get(), abilityId, damage);
        if (!result.isSuccess()) {
            return AbilityExecutionResult.rejected("Combat attack was cancelled");
        }
        return AbilityExecutionResult.success();
    }

    private AbilityExecutionResult executeEffect(
        final AbilityRequest request,
        final AbilityDefinition definition
    ) {
        final String effectIdValue = definition.data().getString("effect_id", "");
        if (effectIdValue.isBlank()) {
            return AbilityExecutionResult.rejected("Effect behavior requires effect_id");
        }
        if (request.targetId() == null) {
            return AbilityExecutionResult.rejected("Effect behavior requires an entity target");
        }

        final AssetId effectId;
        try {
            effectId = AssetId.parse(effectIdValue);
        } catch (final IllegalArgumentException exception) {
            return AbilityExecutionResult.rejected("Invalid effect id: " + effectIdValue);
        }

        final Optional<PlayerSession> targetSession = this.runtime.playerSession(request.targetId());
        if (targetSession.isEmpty()) {
            return AbilityExecutionResult.rejected("Target does not have a player session");
        }

        final EffectContext context = new EffectContext(
            Optional.of(request.actorId()),
            request.targetId()
        );
        final Optional<?> applied = this.runtime.applyEffect(
            targetSession.get(),
            effectId,
            new EffectSource("abilities", definition.id().toString()),
            context
        );
        return applied.isPresent()
            ? AbilityExecutionResult.success()
            : AbilityExecutionResult.rejected("Effect could not be applied");
    }

    private static double calculateDamage(final CombatEntity attacker, final SerializedObject data) {
        final SerializedObject damage = data.getObject("damage").orElse(SerializedObject.empty());
        double value = damage.getDouble("base", 0.0D);

        final Map<String, Double> scaling = damage.getObject("scaling")
            .map(SerializedObject::asMap)
            .map(DataDrivenAbilityExecutor::numericMap)
            .orElse(Map.of());
        for (final Map.Entry<String, Double> entry : scaling.entrySet()) {
            final AssetId statId;
            try {
                statId = AssetId.parse(entry.getKey());
            } catch (final IllegalArgumentException exception) {
                continue;
            }
            value += attacker.attributes().finalValue(statId) * entry.getValue();
        }
        return value;
    }

    private static Map<String, Double> numericMap(final Map<String, Object> values) {
        final Map<String, Double> result = new java.util.LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof Number number) {
                result.put(entry.getKey(), number.doubleValue());
            } else if (entry.getValue() instanceof String stringValue) {
                try {
                    result.put(entry.getKey(), Double.parseDouble(stringValue));
                } catch (final NumberFormatException ignored) {
                    // Ignore invalid optional scaling values.
                }
            }
        }
        return Map.copyOf(result);
    }
}
