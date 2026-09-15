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
    public DataDrivenAbilityExecutor(final AbilityRuntimeGateway runtime) { this.runtime = Objects.requireNonNull(runtime, "runtime"); }

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
        final Optional<CombatEntity> attacker = runtime.combatEntity(request.actorId());
        final Optional<CombatEntity> target = runtime.combatEntity(request.targetId());
        if (attacker.isEmpty() || target.isEmpty()) return AbilityExecutionResult.rejected("Actor or target is not a combat entity");
        if (!attacker.get().health().isAlive()) return AbilityExecutionResult.rejected("Actor is not alive");
        if (!target.get().health().isAlive()) return AbilityExecutionResult.rejected("Target is not alive");
        final double damage = calculateDamage(attacker.get(), definition.data());
        if (!Double.isFinite(damage) || damage < 0.0D) return AbilityExecutionResult.rejected("Ability produced invalid damage");
        final HitResult result = runtime.attack(attacker.get(), target.get(), definition.id(), damage);
        return result.isSuccess() ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("Combat attack was cancelled");
    }

    private AbilityExecutionResult executeAreaAttack(final AbilityRequest request, final AbilityDefinition definition) {
        if (request.targetId() == null) return AbilityExecutionResult.rejected("Area physical attack requires an entity target");
        final Optional<CombatEntity> attacker = runtime.combatEntity(request.actorId());
        if (attacker.isEmpty() || !attacker.get().health().isAlive()) return AbilityExecutionResult.rejected("Actor is not a live combat entity");
        final double radius = definition.data().getDouble("radius", 4.0D);
        if (!Double.isFinite(radius) || radius <= 0.0D) return AbilityExecutionResult.rejected("Area radius must be positive");
        final double damage = calculateDamage(attacker.get(), definition.data());
        if (!Double.isFinite(damage) || damage < 0.0D) return AbilityExecutionResult.rejected("Ability produced invalid damage");
        int resolved = 0;
        for (final CombatEntity target : runtime.nearbyCombatEntities(request.targetId(), radius)) {
            if (target.uniqueId().equals(attacker.get().uniqueId()) || !target.health().isAlive()) continue;
            if (runtime.attack(attacker.get(), target, definition.id(), damage).isSuccess()) resolved++;
        }
        return resolved > 0 ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("No valid targets in area");
    }

    private AbilityExecutionResult executeEffect(final AbilityRequest request, final AbilityDefinition definition) {
        final String effectIdValue = definition.data().getString("effect_id", definition.data().getString("effect", ""));
        if (effectIdValue.isBlank()) return AbilityExecutionResult.rejected("Effect behavior requires effect or effect_id");
        final UUID primaryTarget = definition.targetType() == AbilityTargetType.SELF ? request.actorId() : request.targetId();
        if (primaryTarget == null) return AbilityExecutionResult.rejected("Effect behavior requires an entity target");
        final AssetId effectId;
        try { effectId = AssetId.parse(effectIdValue); }
        catch (IllegalArgumentException exception) { return AbilityExecutionResult.rejected("Invalid effect id: " + effectIdValue); }

        final double radius = definition.targetType() == AbilityTargetType.AREA ? definition.data().getDouble("radius", 8.0D) : 0.0D;
        final Set<UUID> targetIds = new LinkedHashSet<>();
        targetIds.add(primaryTarget);
        if (radius > 0.0D && Double.isFinite(radius)) {
            for (final CombatEntity target : runtime.nearbyCombatEntities(primaryTarget, radius)) targetIds.add(target.uniqueId());
        }
        int appliedCount = 0;
        for (final UUID targetId : targetIds) {
            final Optional<PlayerSession> targetSession = runtime.playerSession(targetId);
            if (targetSession.isEmpty()) continue;
            final Optional<?> applied = runtime.applyEffect(targetSession.get(), effectId,
                new EffectSource("abilities", definition.id().toString()),
                new EffectContext(Optional.of(request.actorId()), targetId));
            if (applied.isPresent()) appliedCount++;
        }
        return appliedCount > 0 ? AbilityExecutionResult.success() : AbilityExecutionResult.rejected("Effect could not be applied");
    }

    private static double calculateDamage(final CombatEntity attacker, final SerializedObject data) {
        final SerializedObject damage = data.getObject("damage").orElse(SerializedObject.empty());
        double value = damage.getDouble("base", 0.0D);
        final Map<String, Double> scaling = damage.getObject("scaling")
            .map(SerializedObject::asMap).map(DataDrivenAbilityExecutor::numericMap).orElse(Map.of());
        for (final Map.Entry<String, Double> entry : scaling.entrySet()) {
            final AssetId statId = resolveStatId(entry.getKey());
            if (statId != null) value += attacker.attributes().finalValue(statId) * entry.getValue();
        }
        return value;
    }

    private static AssetId resolveStatId(final String raw) {
        final String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.equals("attack_damage") || normalized.equals("attackdamage")) return AssetId.parse("ascension:attack_power");
        try { return AssetId.parse(raw); } catch (IllegalArgumentException exception) { return null; }
    }

    private static Map<String, Double> numericMap(final Map<String, Object> values) {
        final Map<String, Double> result = new java.util.LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof Number number) result.put(entry.getKey(), number.doubleValue());
            else if (entry.getValue() instanceof String stringValue) {
                try { result.put(entry.getKey(), Double.parseDouble(stringValue)); } catch (NumberFormatException ignored) { }
            }
        }
        return Map.copyOf(result);
    }
}
