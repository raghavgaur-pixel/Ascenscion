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
import com.ascension.combat.platform.BukkitCombatEntity;
import com.ascension.combat.service.CombatService;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectInstance;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.effects.service.EffectService;
import com.ascension.session.model.PlayerSession;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.attribute.AttributeContainer;
import com.ascension.stats.service.AttributeService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/**
 * Bukkit boundary for the generic ability runtime.
 */
public final class BukkitAbilityRuntimeGateway implements AbilityRuntimeGateway {

    private final PlayerSessionManager sessions;
    private final AttributeService attributes;
    private final CombatService combat;
    private final EffectService effects;

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
        final org.bukkit.entity.Entity entity = Bukkit.getEntity(Objects.requireNonNull(uniqueId, "uniqueId"));
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
            return Optional.empty();
        }

        final AttributeContainer attributeContainer = this.sessions.session(uniqueId)
            .map(PlayerSession::attributes)
            .orElseGet(this.attributes::createContainer);

        final var effectContainer = this.sessions.session(uniqueId)
            .map(PlayerSession::activeEffects)
            .orElseGet(com.ascension.effects.runtime.EffectContainer::new);

        return Optional.of(new BukkitCombatEntity(livingEntity, attributeContainer, effectContainer));
    }

    @Override
    public Optional<PlayerSession> playerSession(final UUID uniqueId) {
        return this.sessions.session(Objects.requireNonNull(uniqueId, "uniqueId"));
    }

    @Override
    public HitResult attack(
        final CombatEntity attacker,
        final CombatEntity target,
        final AssetId abilityId,
        final double damage
    ) {
        requireMainThread();
        final DamageSource source = new DamageSource() {
            @Override
            public String id() {
                return abilityId.toString();
            }

            @Override
            public String name() {
                return abilityId.toString();
            }

            @Override
            public Optional<CombatEntity> entity() {
                return Optional.of(attacker);
            }
        };
        final AttackContext context = new AttackContext(
            Optional.of(attacker),
            new CombatTarget(target),
            new DamageContext(source, DefaultDamageType.PHYSICAL, damage)
        );
        return this.combat.execute(context);
    }

    @Override
    public Optional<EffectInstance> applyEffect(
        final PlayerSession target,
        final AssetId effectId,
        final EffectSource source,
        final EffectContext context
    ) {
        requireMainThread();
        return this.effects.applyEffect(target, effectId, source, context);
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Ability execution against live Bukkit entities must occur on the server thread");
        }
    }
}
