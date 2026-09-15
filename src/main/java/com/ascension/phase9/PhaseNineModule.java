package com.ascension.phase9;

import com.ascension.abilities.platform.BukkitAbilityRuntimeGateway;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.events.EventBus;
import com.ascension.events.EventPriority;
import com.ascension.events.lifecycle.PlayerReadyEvent;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.progression.service.ExperienceCurve;
import com.ascension.progression.service.ProgressionService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.tower.model.FloorId;
import com.ascension.tower.service.TowerService;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;

/** Wires the first playable Phase 9 gameplay services and event bridges. */
public final class PhaseNineModule extends AbstractModule {
    @Override public String id() { return "phase-9"; }
    @Override public Set<String> dependencies() { return Set.of("assets", "registry", "profiles", "sessions", "items", "equipment", "effects", "combat", "abilities"); }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registries = services.require(RegistryHub.class);
        final ProgressionService progression = new ProgressionService(ExperienceCurve.polynomial(100L, 0.15D, 20L), 100);
        final TowerService tower = new TowerService(registries.require(AscensionRegistries.FLOOR_DEFINITIONS));
        final QuestService quests = new QuestService(registries, services.require(PlayerProfileService.class), progression, tower,
            services.require(ItemService.class), services.require(ItemMetadataEncoder.class));
        final BukkitAbilityRuntimeGateway runtime = (BukkitAbilityRuntimeGateway) services.require(com.ascension.abilities.service.AbilityRuntimeGateway.class);
        final MobService mobs = new MobService(registries, runtime, services.require(JavaPlugin.class));

        services.register(ProgressionService.class, progression);
        services.register(TowerService.class, tower);
        services.register(QuestService.class, quests);
        services.register(MobService.class, mobs);

        final EventBus events = services.require(EventBus.class);
        final GameplayCombatListener combatListener = new GameplayCombatListener(quests, registries, services);
        events.subscribe("phase-9", com.ascension.combat.event.EntityKilledEvent.class, EventPriority.NORMAL, false, combatListener::onKilled);
        events.subscribe("phase-9", PlayerReadyEvent.class, EventPriority.NORMAL, false, event -> {
            final var profile = event.session().profile().orElse(null);
            if (profile == null) return;
            final var floors = profile.components().find("unlocked_floors").map(UnlockedFloorsProfileComponent.class::cast).orElse(null);
            if (floors != null && tower.unlock(floors, new FloorId("ascension:floor_001")).status() != TowerService.UnlockResult.Status.REJECTED) {
                profile.components().find("quests").ifPresent(ignored -> quests.accept(profile.uniqueId(), "ascension:arrival"));
            }
            services.require(PlayerProfileService.class).save(profile.uniqueId());
        });
        services.require(JavaPlugin.class).getServer().getPluginManager().registerEvents(new BukkitGameplayListener(mobs), services.require(JavaPlugin.class));
        registerCommand(services, new PhaseNineCommand(quests, mobs, services.require(PlayerSessionManager.class), tower));
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.find(EventBus.class).ifPresent(events -> events.unsubscribeOwner("phase-9"));
        services.find(MobService.class).ifPresent(MobService::clear);
    }

    private static void registerCommand(final ServiceRegistry services, final PhaseNineCommand phaseCommand) {
        final JavaPlugin plugin = services.require(JavaPlugin.class);
        final var command = plugin.getCommand("asc");
        if (command == null) return;
        final CommandExecutor previous = command.getExecutor();
        command.setExecutor(new CommandExecutor() {
            @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
                if (phaseCommand.handles(args)) return phaseCommand.onCommand(sender, cmd, label, args);
                return previous != null && previous.onCommand(sender, cmd, label, args);
            }
        });
    }

    private static final class BukkitGameplayListener implements Listener {
        private final MobService mobs;
        private BukkitGameplayListener(final MobService mobs) { this.mobs = mobs; }
        @EventHandler public void onEntityDeath(final EntityDeathEvent event) { this.mobs.forget(event.getEntity().getUniqueId()); }
    }
}
