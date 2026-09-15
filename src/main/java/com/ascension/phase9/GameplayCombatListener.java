package com.ascension.phase9;

import com.ascension.assets.model.AssetId;
import com.ascension.combat.event.EntityKilledEvent;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.platform.BukkitCombatEntity;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/** Bridges combat deaths from live Bukkit entities into Phase 9 progression. */
public final class GameplayCombatListener {

    private final QuestService quests;
    private final RegistryHub registries;
    private final NamespacedKey mobKey;
    private final NamespacedKey bossKey;

    public GameplayCombatListener(final QuestService quests, final RegistryHub registries, final ServiceRegistry services) {
        this.quests = quests;
        this.registries = registries;
        final org.bukkit.plugin.java.JavaPlugin plugin = services.require(org.bukkit.plugin.java.JavaPlugin.class);
        this.mobKey = new NamespacedKey(plugin, "mob_id");
        this.bossKey = new NamespacedKey(plugin, "boss_id");
    }

    public void onKilled(final EntityKilledEvent event) {
        final UUID killerId = event.killer().map(CombatEntity::uniqueId).orElse(null);
        if (killerId == null || !(event.entity() instanceof BukkitCombatEntity killed)) return;
        final LivingEntity entity = killed.entity();
        final String mobValue = entity.getPersistentDataContainer().get(this.mobKey, PersistentDataType.STRING);
        final String bossValue = entity.getPersistentDataContainer().get(this.bossKey, PersistentDataType.STRING);
        final AssetId mobId = parse(mobValue);
        final AssetId bossId = parse(bossValue);
        if (mobId != null && this.registries.getOrCreate(AscensionRegistries.MOB_DEFINITIONS).find(mobId).isPresent()) {
            this.quests.grantKillRewards(killerId, mobId, false);
        }
        if (bossId != null && this.registries.getOrCreate(AscensionRegistries.BOSS_DEFINITIONS).find(bossId).isPresent()) {
            this.quests.grantKillRewards(killerId, bossId, true);
        }
        if (mobValue != null || bossValue != null) {
            this.quests.defeat(killerId, mobValue, bossValue);
        }
    }

    private static AssetId parse(final String value) {
        if (value == null || value.isBlank()) return null;
        try { return AssetId.parse(value); }
        catch (IllegalArgumentException exception) { return null; }
    }
}
