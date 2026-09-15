package com.ascension.phase9;

import com.ascension.abilities.platform.BukkitAbilityRuntimeGateway;
import com.ascension.assets.definition.BossDefinition;
import com.ascension.assets.definition.MobDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

/** Authoritative mob bridge. Uses vanilla models as temporary placeholders until custom models are added. */
public final class MobService {
    private final RegistryHub registries;
    private final BukkitAbilityRuntimeGateway runtime;
    private final NamespacedKey mobKey;
    private final NamespacedKey bossKey;

    public MobService(final RegistryHub registries, final BukkitAbilityRuntimeGateway runtime, final JavaPlugin plugin) {
        this.registries = Objects.requireNonNull(registries, "registries");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.mobKey = new NamespacedKey(plugin, "mob_id");
        this.bossKey = new NamespacedKey(plugin, "boss_id");
    }

    public Optional<LivingEntity> spawnMob(final AssetId id, final Location location) {
        if (id == null || location == null || location.getWorld() == null) return Optional.empty();
        MobDefinition definition = null;
        try { definition = this.registries.getOrCreate(AscensionRegistries.MOB_DEFINITIONS).find(id).orElse(null); } catch (RuntimeException ignored) { }
        final String model = definition == null ? placeholderFor(id) : definition.data().getString("entity_type", placeholderFor(id));
        final double health = definition == null ? 30.0D : Math.max(1.0D, definition.data().getDouble("health", 30.0D));
        return spawn(location, model, definition == null ? prettyName(id) : definition.displayName()).map(entity -> {
            entity.getPersistentDataContainer().set(this.mobKey, PersistentDataType.STRING, id.toString());
            this.runtime.registerRuntime(entity.getUniqueId(), health);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            return entity;
        });
    }

    public Optional<LivingEntity> spawnBoss(final AssetId id, final Location location) {
        if (id == null || location == null || location.getWorld() == null) return Optional.empty();
        BossDefinition definition = null;
        try { definition = this.registries.getOrCreate(AscensionRegistries.BOSS_DEFINITIONS).find(id).orElse(null); } catch (RuntimeException ignored) { }
        final String model = definition == null ? "IRON_GOLEM" : definition.data().getString("entity_type", "IRON_GOLEM");
        final double health = definition == null ? 500.0D : Math.max(1.0D, definition.data().getDouble("health", 500.0D));
        return spawn(location, model, definition == null ? prettyName(id) : definition.displayName()).map(entity -> {
            entity.getPersistentDataContainer().set(this.bossKey, PersistentDataType.STRING, id.toString());
            this.runtime.registerRuntime(entity.getUniqueId(), health);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            return entity;
        });
    }

    public Optional<AssetId> mobId(final Entity entity) { return parse(entity.getPersistentDataContainer().get(this.mobKey, PersistentDataType.STRING)); }
    public Optional<AssetId> bossId(final Entity entity) { return parse(entity.getPersistentDataContainer().get(this.bossKey, PersistentDataType.STRING)); }
    public void forget(final UUID id) { this.runtime.forget(id); }
    public void clear() { this.runtime.clearAll(); }

    private static Optional<LivingEntity> spawn(final Location location, final String entityTypeName, final String name) {
        final EntityType type;
        try { type = EntityType.valueOf(entityTypeName.toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException exception) { return Optional.empty(); }
        final Entity spawned = location.getWorld().spawnEntity(location, type);
        if (!(spawned instanceof LivingEntity living)) return Optional.empty();
        living.setCustomName("§c" + name);
        living.setCustomNameVisible(true);
        return Optional.of(living);
    }

    private static String placeholderFor(final AssetId id) {
        final String value = id.toString().toLowerCase(java.util.Locale.ROOT);
        if (value.contains("wolf")) return "WOLF";
        if (value.contains("beetle")) return "SPIDER";
        return "ZOMBIE";
    }

    private static String prettyName(final AssetId id) {
        final String value = id.value().replace('_', ' ');
        return value.isBlank() ? "Ascension Mob" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static Optional<AssetId> parse(final String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try { return Optional.of(AssetId.parse(value)); } catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
}
