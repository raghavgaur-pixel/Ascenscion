package com.ascension.combat.platform;

import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.RuntimeHealth;
import com.ascension.effects.runtime.EffectContainer;
import com.ascension.stats.attribute.AttributeContainer;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;

/**
 * Main-thread adapter from a live Bukkit living entity to the Ascension combat model.
 */
public final class BukkitCombatEntity implements CombatEntity {

    private final LivingEntity entity;
    private final AttributeContainer attributes;
    private final EffectContainer effects;
    private final RuntimeHealth health;

    public BukkitCombatEntity(
        final LivingEntity entity,
        final AttributeContainer attributes,
        final EffectContainer effects
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.health = new BukkitRuntimeHealth(this.entity);
    }

    @Override
    public UUID uniqueId() {
        return this.entity.getUniqueId();
    }

    @Override
    public String name() {
        final String customName = this.entity.getCustomName();
        return customName == null || customName.isBlank() ? this.entity.getName() : customName;
    }

    @Override
    public RuntimeHealth health() {
        return this.health;
    }

    @Override
    public AttributeContainer attributes() {
        return this.attributes;
    }

    @Override
    public EffectContainer effects() {
        return this.effects;
    }

    public LivingEntity entity() {
        return this.entity;
    }

    private static final class BukkitRuntimeHealth implements RuntimeHealth {

        private final LivingEntity entity;

        private BukkitRuntimeHealth(final LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public double current() {
            return this.entity.getHealth();
        }

        @Override
        @SuppressWarnings("deprecation")
        public double maximum() {
            return this.entity.getMaxHealth();
        }

        @Override
        public void set(final double amount) {
            requireMainThread();
            this.entity.setHealth(clamp(amount));
        }

        @Override
        public void heal(final double amount) {
            requireMainThread();
            if (amount <= 0.0D) {
                return;
            }
            set(current() + amount);
        }

        @Override
        public void damage(final double amount) {
            requireMainThread();
            if (amount <= 0.0D) {
                return;
            }
            set(current() - amount);
        }

        @Override
        public boolean isAlive() {
            return !this.entity.isDead() && current() > 0.0D;
        }

        private double clamp(final double amount) {
            if (!Double.isFinite(amount)) {
                throw new IllegalArgumentException("Health must be finite");
            }
            return Math.max(0.0D, Math.min(maximum(), amount));
        }

        private static void requireMainThread() {
            if (!org.bukkit.Bukkit.isPrimaryThread()) {
                throw new IllegalStateException("Live Bukkit health mutation must occur on the server thread");
            }
        }
    }
}
