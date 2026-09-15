package com.ascension.abilities.service;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.model.AbilityTargetType;
import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.HitResult;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.serialization.SerializedObject;
import com.ascension.session.model.PlayerSession;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Executes generic authored ability behaviors. */
public final class DataDrivenAbilityExecutor implements AbilityExecutor {

    private final AbilityRuntimeGateway runtime;

    public DataDrivenAbilityExecutor(final AbilityRuntimeGateway runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public AbilityExecutionResult execute(final AbilityRequest request, final AbilityDefinition definition) {
        final String behavior = definition.data().getString("behavior", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (behavior.isBlank()) return AbilityExecutionResult.rejected("Ability has no behavior");
        return switch (behavior) {
            case "physical_attack" -> executeSingleAttack(request, definition);
            case "area_physical_attack" -> executeAreaAttack(request, definition);
            case "apply_effect" -> executeEffect(request, definition);
            default -> AbilityExecutionResult.rejected("Unsupported ability behavior: " + behavior);
        };
    }

    private AbilityExecutionResult executeSingleAttack(final AbilityRequest request, final AbilityDefinition definition) {
        if (request.targetId() == null) return AbilityExecutionResult.rejected("Physical attack requires an entity target");
        final Optional<CombatEntity> attacker = this.runtime.combatEntity(request.actorId());
        final Optional<CombatEntity> target = this.runtime.combatEntity(request.targetId());
        if (attacker.isEmpty()) return AbilityExecutionResult.rejected("Actor is not a combat entity");
        if (target.isEmpty()) return AbilityExecutionResult.rejected("Target is not a combat entity");
        if (!attacker.get().health().isAlive()) return AbilityExecutionResult.rejected("Actor is not alive");
        if (!target.get().health().isAlive()) return AbilityExecutionResult.rejected("Target is not alive");
        final double damage = calculateDamage(attacker.get(), definition.data());
        if (!Double.isFinite(damage) || damage < 0.0D) return AbilityExecutionResult.rejected("Ability produced invalid damage");
        final HitResult result = this.runtime.attack(attacker.get(), target.get(), definition.id(), damage);
        return result.isSuccess() ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("Combat attack was cancelled");
    }

    private AbilityExecutionResult executeAreaAttack(final AbilityRequest request, final AbilityDefinition definition) {
        if (request.targetId() == null) return AbilityExecutionResult.rejected("Area physical attack requires an entity center target");
        final Optional<CombatEntity> attacker = this.runtime.combatEntity(request.actorId());
        if (attacker.isEmpty()) return AbilityExecutionResult.rejected("Actor is not a combat entity");
        if (!attacker.get().health().isAlive()) return AbilityExecutionResult.rejected("Actor is not alive");
        final double radius = definition.data().getDouble("radius", 4.0D);
        if (!Double.isFinite(radius) || radius <= 0.0D) return AbilityExecutionResult.rejected("Area radius must be positive");
        final double damage = calculateDamage(attacker.get(), definition.data());
        if (!Double.isFinite(damage) || damage < 0.0D) return AbilityExecutionResult.rejected("Ability produced invalid damage");

        int resolved = 0;
        for (final CombatEntity target : this.runtime.nearbyCombatEntities(request.targetId(), radius)) {
            if (!target.uniqueId().equals(attacker.get().uniqueId()) && target.health().isAlive()) {
                if (this.runtime.attack(attacker.get(), target, definition.id(), damage).isSuccess()) resolved++;
            }
        }
        return resolved > 0 ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("No valid targets in area");
    }

    private AbilityExecutionResult executeEffect(final AbilityRequest request, final AbilityDefinition definition) {
        final String effectIdValue = definition.data().getString("effect_id", definition.data().getString("effect", ""));
        if (effectIdValue.isBlank()) return AbilityExecutionResult.rejected("Effect behavior requires effect or effect_id");
        if (request.targetId() == null) return AbilityExecutionResult.rejected("Effect behavior requires an entity target");
        final AssetId effectId;
        try { effectId = AssetId.parse(effectIdValue); }
        catch (final IllegalArgumentException exception) { return AbilityExecutionResult.rejected("Invalid effect id: " + effectIdValue); }

        final double radius = definition.targetType() == AbilityTargetType.AREA
            ? definition.data().getDouble("radius", 8.0D) : 0.0D;
        final Set<UUID> targetIds = new LinkedHashSet<>();
        targetIds.add(request.targetId());
        if (radius > 0.0D && Double.isFinite(radius)) {
            for (final CombatEntity target : this.runtime.nearbyCombatEntities(request.targetId(), radius)) {
                targetIds.add(target.uniqueId());
            }
        }

        int appliedCount = 0;
        for (final UUID targetId : targetIds) {
            final Optional<PlayerSession> targetSession = this.runtime.playerSession(targetId);
            if (targetSession.isEmpty()) continue;
            final EffectContext context = new EffectContext(Optional.of(request.actorId()), targetId);
            final Optional<?> applied = this.runtime.applyEffect(
                targetSession.get(), effectId,
                new EffectSource("abilities", definition.id().toString()), context
            );
            if (applied.isPresent()) appliedCount++;
        }
        return appliedCount > 0 ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("Effect could not be applied");
    }

    private static double calculateDamage(final CombatEntity attacker, final SerializedObject data) {
        final SerializedObject damage = data.getObject("damage").orElse(SerializedObject.empty());
        double value = damage.getDouble("base", 0.0D);
        final Map<String, Double> scaling = damage.getObject("scaling")
            .map(SerializedObject::asMap)
            .map(DataDrivenAbilityExecutor::numericMap)
            .orElse(Map.of());
        for (final Map.Entry<String, Double> entry : scaling.entrySet()) {
            try {
                final AssetId statId = AssetId.parse(entry.getKey());
                value += attacker.attributes().finalValue(statId) * entry.getValue();
            } catch (final IllegalArgumentException ignored) { }
        }
        return value;
    }

    private static Map<String, Double> numericMap(final Map<String, Object> values) {
        final Map<String, Double> result = new java.util.LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof Number number) result.put(entry.getKey(), number.doubleValue());
            else if (entry.getValue() instanceof String stringValue) {
                try { result.put(entry.getKey(), Double.parseDouble(stringValue)); }
                catch (final NumberFormatException ignored) { }
            }
        }
        return Map.copyOf(result);
    }
}
