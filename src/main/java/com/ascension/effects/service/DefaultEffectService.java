package com.ascension.effects.service;

import com.ascension.assets.model.AssetId;
import com.ascension.effects.definition.EffectDefinition;
import com.ascension.effects.definition.EffectModifierDefinition;
import com.ascension.effects.event.EffectAppliedEvent;
import com.ascension.effects.event.EffectExpiredEvent;
import com.ascension.effects.event.EffectRefreshedEvent;
import com.ascension.effects.event.EffectRemovedEvent;
import com.ascension.effects.event.EffectStackChangedEvent;
import com.ascension.effects.event.EffectTickEvent;
import com.ascension.effects.model.EffectStackingBehavior;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectInstance;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.events.EventBus;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.runtime.tick.TickContext;
import com.ascension.runtime.tick.Tickable;
import com.ascension.session.model.PlayerSession;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.stats.modifier.AttributeModifier;
import com.ascension.stats.modifier.ModifierRemovalPolicy;
import com.ascension.stats.modifier.ModifierSource;
import com.ascension.stats.modifier.ModifierStackingBehavior;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DefaultEffectService implements EffectService, Tickable {

    private final RegistryHub registryHub;
    private final EventBus eventBus;
    private final PlayerSessionManager sessionManager;

    public DefaultEffectService(final RegistryHub registryHub, final EventBus eventBus, final PlayerSessionManager sessionManager) {
        this.registryHub = java.util.Objects.requireNonNull(registryHub, "registryHub");
        this.eventBus = java.util.Objects.requireNonNull(eventBus, "eventBus");
        this.sessionManager = java.util.Objects.requireNonNull(sessionManager, "sessionManager");
    }

    @Override
    public Optional<EffectInstance> applyEffect(PlayerSession session, AssetId effectId, EffectSource source, EffectContext context) {
        final Optional<EffectDefinition> definitionOpt = this.registryHub
            .require(AscensionRegistries.EFFECT_DEFINITIONS)
            .find(effectId);

        if (definitionOpt.isEmpty()) {
            return Optional.empty();
        }

        final EffectDefinition definition = definitionOpt.get();
        final List<EffectInstance> existingInstances = session.activeEffects().getByEffectId(effectId);
        final Instant now = Instant.now();
        final Optional<Instant> expiresAt = definition.baseDuration().map(now::plus);

        if (!existingInstances.isEmpty()) {
            final EffectStackingBehavior behavior = definition.stackingBehavior();
            final EffectInstance existing = existingInstances.get(0);

            switch (behavior) {
                case REFRESH_DURATION -> {
                    existing.setExpiresAt(expiresAt);
                    this.eventBus.publish(new EffectRefreshedEvent(session, existing));
                    return Optional.of(existing);
                }
                case MAXIMUM_STACKS -> {
                    if (existing.currentStacks() < definition.maxStacks()) {
                        final int oldStacks = existing.currentStacks();
                        existing.setStacks(oldStacks + 1);
                        existing.setExpiresAt(expiresAt);
                        applyModifiers(session, existing); // Recalculate
                        this.eventBus.publish(new EffectStackChangedEvent(session, existing, oldStacks, existing.currentStacks()));
                    } else {
                        existing.setExpiresAt(expiresAt);
                        this.eventBus.publish(new EffectRefreshedEvent(session, existing));
                    }
                    return Optional.of(existing);
                }
                case REPLACE_EXISTING -> {
                    removeEffect(session, existing.instanceId());
                }
                case INDEPENDENT_STACKS -> {
                    // Falls through to create new
                }
                case PRIORITY_OVERRIDE, CUSTOM_MERGE_STRATEGY -> {
                    // Not fully implemented yet, default to replace
                    removeEffect(session, existing.instanceId());
                }
            }
        }

        final EffectInstance newInstance = new EffectInstance(
            effectId,
            definition,
            source,
            context,
            now,
            expiresAt,
            1
        );

        session.activeEffects().add(newInstance);
        applyModifiers(session, newInstance);
        this.eventBus.publish(new EffectAppliedEvent(session, newInstance));

        return Optional.of(newInstance);
    }

    @Override
    public boolean removeEffect(PlayerSession session, UUID instanceId) {
        final Optional<EffectInstance> removed = session.activeEffects().remove(instanceId);
        if (removed.isPresent()) {
            removeModifiers(session, removed.get());
            this.eventBus.publish(new EffectRemovedEvent(session, removed.get()));
            return true;
        }
        return false;
    }

    private void applyModifiers(final PlayerSession session, final EffectInstance instance) {
        final AttributeContainer container = session.attributes();
        final ModifierSource modifierSource = new ModifierSource("effects", "effect_instance", instance.instanceId().toString());

        for (final EffectModifierDefinition modDef : instance.definition().modifiers()) {
            final String modId = instance.instanceId().toString() + "_" + modDef.statId().value();
            final double value = modDef.operation() == com.ascension.stats.modifier.ModifierOperation.MULTIPLY
                ? Math.pow(modDef.value(), instance.currentStacks())
                : modDef.value() * instance.currentStacks();

            final AttributeModifier modifier = new AttributeModifier(
                modId,
                "effects",
                modifierSource,
                instance.definition().category(),
                0,
                modDef.operation(),
                value,
                instance.appliedAt(),
                instance.expiresAt(),
                ModifierStackingBehavior.REPLACE_BY_ID,
                ModifierRemovalPolicy.MANUAL
            );
            container.addModifier(modDef.statId(), modifier);
        }
    }

    private void removeModifiers(final PlayerSession session, final EffectInstance instance) {
        session.attributes().removeSource(instance.instanceId().toString());
    }

    @Override
    public boolean dispelEffect(PlayerSession session, UUID instanceId) {
        // To be implemented
        return false;
    }

    @Override
    public void clearEffects(PlayerSession session) {
        final List<EffectInstance> active = new ArrayList<>(session.activeEffects().snapshot());
        for (final EffectInstance instance : active) {
            removeEffect(session, instance.instanceId());
        }
    }

    @Override
    public void tick(TickContext context) {
        final Instant now = Instant.now();

        for (final PlayerSession session : this.sessionManager.sessions()) {
            final List<EffectInstance> toRemove = new ArrayList<>();
            final List<EffectInstance> toTick = new ArrayList<>();

            // Using snapshot is fine here to avoid ConcurrentModificationExceptions
            // since we are likely modifying the container inside `removeEffect` calls
            for (final EffectInstance instance : session.activeEffects().snapshot()) {
                if (instance.isExpired(now)) {
                    toRemove.add(instance);
                } else if (instance.definition().durationType() == com.ascension.effects.model.EffectDurationType.PERIODIC) {
                    toTick.add(instance);
                }
            }

            for (final EffectInstance expired : toRemove) {
                if (removeEffect(session, expired.instanceId())) {
                    this.eventBus.publish(new EffectExpiredEvent(session, expired));
                }
            }

            for (final EffectInstance periodic : toTick) {
                this.eventBus.publish(new EffectTickEvent(session, periodic));
            }
        }
    }
}
