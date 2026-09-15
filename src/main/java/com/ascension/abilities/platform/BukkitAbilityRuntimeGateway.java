package com.ascension.abilities.platform;

import com.ascension.abilities.service.AbilityRuntimeGateway;
import com.ascension.assets.model.AssetId;
import com.ascension.combat.model.AttackContext;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.CombatTarget;
import com.ascension.combat.model.DamageContext;
import com.ascension.combat.model.DamageSource;
import com.ascension.combat.model.DefaultDamageType;
import com.ascension.combat.model.HitResult;
import com.ascension.combat.model.RuntimeHealth;
import com.ascension.combat.platform.BukkitCombatEntity;
import com.ascension.combat.service.CombatService;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectContainer;
import com.ascension.effects.runtime.EffectInstance;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.session.model.PlayerSession;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.stats.service.AttributeService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/** Bukkit boundary for the generic ability runtime. */
public final class BukkitAbilityRuntimeGateway implements AbilityRuntimeGateway {

    private final PlayerSessionManager sessions;
    private final AttributeService attributes;
    private final CombatService combat;
    private final EffectService effects;
    private final Map<UUID, MobRuntimeState> mobState = new ConcurrentHashMap<>();

    public BukkitAbilityRuntimeGateway(
        final PlayerSessionManager sessions,
        final AttributeService attributes,
        final CombatService combat,
        final EffectService effects
    ) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.combat = Objects.requireNonNull(combat, "combat");
        this.effects = Objects.requireNonNull(effects, "effects");
    }

    @Override
    public Optional<CombatEntity> combatEntity(final UUID uniqueId) {
        requireMainThread();
        final UUID id = Objects.requireNonNull(uniqueId, "uniqueId");
        final org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
            this.mobState.remove(id);
            return Optional.empty();
        }
        final Optional<PlayerSession> session = this.sessions.session(id);
        if (session.isPresent()) {
            return Optional.of(new BukkitCombatEntity(livingEntity, session.get().attributes(), session.get().activeEffects()));
        }
        final MobRuntimeState state = this.mobState.computeIfAbsent(id, ignored ->
            new MobRuntimeState(this.attributes.createContainer(), new EffectContainer(), new BukkitRuntimeHealth(livingEntity, livingEntity.getMaxHealth()))
        );
        return Optional.of(new BukkitCombatEntity(livingEntity, state.attributes(), state.effects(), state.health()));
    }

    @Override
    public Collection<CombatEntity> nearbyCombatEntities(final UUID originId, final double radius) {
        requireMainThread();
        if (!Double.isFinite(radius) || radius <= 0.0D) return java.util.List.of();
        final CombatEntity origin = this.combatEntity(originId).orElse(null);
        if (!(origin instanceof BukkitCombatEntity bukkitOrigin)) return java.util.List.of();
        final double radiusSquared = radius * radius;
        final Map<UUID, CombatEntity> result = new LinkedHashMap<>();
        for (final org.bukkit.entity.Entity entity : bukkitOrigin.entity().getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.isDead()) continue;
            if (living.getUniqueId().equals(originId)) continue;
            if (living.getLocation().distanceSquared(bukkitOrigin.entity().getLocation()) > radiusSquared) continue;
            this.combatEntity(living.getUniqueId()).ifPresent(target -> result.put(target.uniqueId(), target));
        }
        return java.util.List.copyOf(result.values());
    }

    @Override
    public Optional<PlayerSession> playerSession(final UUID uniqueId) {
        return this.sessions.session(Objects.requireNonNull(uniqueId, "uniqueId"));
    }

    @Override
    public HitResult attack(final CombatEntity attacker, final CombatEntity target, final AssetId abilityId, final double damage) {
        requireMainThread();
        final DamageSource source = new DamageSource() {
            @Override public String id() { return abilityId.toString(); }
            @Override public String name() { return abilityId.toString(); }
            @Override public Optional<CombatEntity> entity() { return Optional.of(attacker); }
        };
        return this.combat.execute(new AttackContext(
            Optional.of(attacker), new CombatTarget(target),
            new DamageContext(source, DefaultDamageType.PHYSICAL, damage)
        ));
    }

    @Override
    public Optional<EffectInstance> applyEffect(
        final PlayerSession target, final AssetId effectId, final EffectSource source, final EffectContext context
    ) {
        requireMainThread();
        return this.effects.applyEffect(target, effectId, source, context);
    }

    public void registerRuntime(final UUID uniqueId, final double maximumHealth) {
        requireMainThread();
        final UUID id = Objects.requireNonNull(uniqueId, "uniqueId");
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0D) {
            throw new IllegalArgumentException("maximumHealth must be positive and finite");
        }
        final org.bukkit.entity.Entity entity = Bukkit.getEntity(id);
        if (!(entity instanceof LivingEntity livingEntity)) return;
        final MobRuntimeState state = new MobRuntimeState(
            this.attributes.createContainer(), new EffectContainer(), new BukkitRuntimeHealth(livingEntity, maximumHealth)
        );
        this.mobState.put(id, state);
    }

    public void forget(final UUID uniqueId) {
        this.mobState.remove(Objects.requireNonNull(uniqueId, "uniqueId"));
    }

    public void clearAll() { this.mobState.clear(); }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Ability execution against live Bukkit entities must occur on the server thread");
    }

    private static final class BukkitRuntimeHealth implements RuntimeHealth {
        private final LivingEntity entity;
        private final double maximum;
        private double current;
        private BukkitRuntimeHealth(final LivingEntity entity, final double maximum) {
            this.entity = entity;
            this.maximum = maximum;
            this.current = maximum;
        }
        @Override public double current() { return current; }
        @Override public double maximum() { return maximum; }
        @Override public void set(final double amount) {
            requireMainThread();
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Health must be finite");
            this.current = Math.max(0.0D, Math.min(this.maximum, amount));
            final double visualMax = this.entity.getMaxHealth();
            final double visual = this.current <= 0.0D ? 0.0D : Math.min(visualMax, visualMax * (this.current / this.maximum));
            this.entity.setHealth(visual);
            if (this.current <= 0.0D) this.entity.setHealth(0.0D);
        }
        @Override public void heal(final double amount) { requireMainThread(); if (amount > 0.0D) set(this.current + amount); }
        @Override public void damage(final double amount) { requireMainThread(); if (amount > 0.0D) set(this.current - amount); }
        @Override public boolean isAlive() { return !this.entity.isDead() && this.current > 0.0D; }
    }

    private record MobRuntimeState(AttributeContainer attributes, EffectContainer effects, RuntimeHealth health) {}
}
