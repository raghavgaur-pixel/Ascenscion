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

/** Minimal authoritative spawn/runtime bridge for authored Floor 1 mobs and bosses. */
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
        final MobDefinition definition = this.registries.getOrCreate(AscensionRegistries.MOB_DEFINITIONS).find(id).orElse(null);
        if (definition == null) return Optional.empty();
        return spawn(location, definition.data().getString("entity_type", defaultEntity(id)), definition.displayName())
            .map(entity -> {
                entity.getPersistentDataContainer().set(this.mobKey, PersistentDataType.STRING, id.toString());
                this.runtime.registerRuntime(entity.getUniqueId(), Math.max(1.0D, definition.data().getDouble("health", 20.0D)));
                entity.setRemoveWhenFarAway(false);
                return entity;
            });
    }

    public Optional<LivingEntity> spawnBoss(final AssetId id, final Location location) {
        final BossDefinition definition = this.registries.getOrCreate(AscensionRegistries.BOSS_DEFINITIONS).find(id).orElse(null);
        if (definition == null) return Optional.empty();
        return spawn(location, definition.data().getString("entity_type", "IRON_GOLEM"), definition.displayName())
            .map(entity -> {
                entity.getPersistentDataContainer().set(this.bossKey, PersistentDataType.STRING, id.toString());
                this.runtime.registerRuntime(entity.getUniqueId(), Math.max(1.0D, definition.data().getDouble("health", 20.0D)));
                entity.setRemoveWhenFarAway(false);
                return entity;
            });
    }

    public Optional<AssetId> mobId(final Entity entity) {
        final String value = entity.getPersistentDataContainer().get(this.mobKey, PersistentDataType.STRING);
        return parse(value);
    }

    public Optional<AssetId> bossId(final Entity entity) {
        final String value = entity.getPersistentDataContainer().get(this.bossKey, PersistentDataType.STRING);
        return parse(value);
    }

    public void forget(final UUID id) { this.runtime.forget(id); }
    public void clear() { this.runtime.clearAll(); }

    private static Optional<LivingEntity> spawn(final Location location, final String entityTypeName, final String name) {
        final EntityType type;
        try { type = EntityType.valueOf(entityTypeName.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
        final Entity spawned = location.getWorld() == null ? null : location.getWorld().spawnEntity(location, type);
        if (!(spawned instanceof LivingEntity living)) return Optional.empty();
        living.setCustomName(name);
        living.setCustomNameVisible(true);
        return Optional.of(living);
    }

    private static String defaultEntity(final AssetId id) {
        final String path = id.value().toLowerCase(java.util.Locale.ROOT);
        return path.contains("wolf") ? "WOLF" : path.contains("beetle") ? "IRON_GOLEM" : "ZOMBIE";
    }

    private static Optional<AssetId> parse(final String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try { return Optional.of(AssetId.parse(value)); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
}
