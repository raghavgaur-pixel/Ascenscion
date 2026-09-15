package com.ascension.phase9;

import com.ascension.abilities.platform.BukkitAbilityRuntimeGateway;
import com.ascension.abilities.service.AbilityService;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;

/** Wires Phase 9 gameplay and the playable Floor 1 slice. */
public final class PhaseNineModule extends AbstractModule {
    @Override public String id() { return "phase-9"; }
    @Override public Set<String> dependencies() { return Set.of("assets", "registry", "profiles", "session", "items", "equipment", "effects", "combat", "abilities"); }

    @Override protected void onStart(final ServiceRegistry services) {
        final RegistryHub registries = services.require(RegistryHub.class);
        final ProgressionService progression = new ProgressionService(ExperienceCurve.polynomial(100L, 0.15D, 20L), 100);
        final TowerService tower = new TowerService(registries.require(AscensionRegistries.FLOOR_DEFINITIONS));
        final QuestService quests = new QuestService(registries, services.require(PlayerProfileService.class), progression, tower, services.require(ItemService.class), services.require(ItemMetadataEncoder.class));
        final QuestMenuService journal = new QuestMenuService(quests, registries);
        final BukkitAbilityRuntimeGateway runtime = (BukkitAbilityRuntimeGateway) services.require(com.ascension.abilities.service.AbilityRuntimeGateway.class);
        final JavaPlugin plugin = services.require(JavaPlugin.class);
        final MobService mobs = new MobService(registries, runtime, plugin);

        final PremiumFloorOneBuilder premiumBuilder = new PremiumFloorOneBuilder(plugin);
        premiumBuilder.prepareFreshWorld();

        final FloorOneWorldService floorWorld = new FloorOneWorldService(plugin, quests, mobs, services.require(AbilityService.class), services.require(ItemService.class), services.require(ItemMetadataEncoder.class), journal);
        services.register(ProgressionService.class, progression);
        services.register(TowerService.class, tower);
        services.register(QuestService.class, quests);
        services.register(MobService.class, mobs);
        services.register(QuestMenuService.class, journal);
        services.register(FloorOneWorldService.class, floorWorld);

        plugin.getServer().getPluginManager().registerEvents(journal, plugin);
        floorWorld.start();
        premiumBuilder.build();

        final EventBus events = services.require(EventBus.class);
        final GameplayCombatListener combatListener = new GameplayCombatListener(quests, registries, services);
        events.subscribe("phase-9", com.ascension.combat.event.EntityKilledEvent.class, EventPriority.NORMAL, false, combatListener::onKilled);
        events.subscribe("phase-9", PlayerReadyEvent.class, EventPriority.NORMAL, false,
            event -> initializeFloorOnePlayer(event.session().profile().map(profile -> profile.uniqueId()).orElse(null), quests, tower, floorWorld, services));
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler public void onJoin(final PlayerJoinEvent event) { bootstrapRetry(plugin, event.getPlayer(), quests, tower, floorWorld, services, 0); }
        }, plugin);
        registerCommand(services, new PhaseNineCommand(quests, mobs, services.require(PlayerSessionManager.class), tower, journal));
        final var questsCommand = plugin.getCommand("quests");
        if (questsCommand != null) questsCommand.setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) { journal.open(player); return true; }
            sender.sendMessage(Component.text("This command requires a player."));
            return true;
        });
    }

    private static void bootstrapRetry(final JavaPlugin plugin, final Player player, final QuestService quests, final TowerService tower, final FloorOneWorldService floorWorld, final ServiceRegistry services, final int attempt) {
        if (attempt == 0) { quests.grantStarterItems(player.getUniqueId()); floorWorld.preparePlayer(player); }
        if (attempt >= 10 || !player.isOnline()) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            final boolean ready = initializeFloorOnePlayer(player.getUniqueId(), quests, tower, floorWorld, services);
            if (!ready) bootstrapRetry(plugin, player, quests, tower, floorWorld, services, attempt + 1);
        }, 20L);
    }

    private static boolean initializeFloorOnePlayer(final java.util.UUID playerId, final QuestService quests, final TowerService tower, final FloorOneWorldService floorWorld, final ServiceRegistry services) {
        if (playerId == null) return false;
        final var profile = services.require(PlayerProfileService.class).online(playerId).orElse(null);
        final Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (profile == null || player == null || !player.isOnline()) return false;
        final var floors = profile.components().find("unlocked_floors").map(UnlockedFloorsProfileComponent.class::cast).orElse(null);
        if (floors != null) tower.unlock(floors, new FloorId("ascension:floor_001"));
        quests.accept(playerId, QuestService.ARRIVAL);
        quests.grantStarterItems(playerId);
        floorWorld.preparePlayer(player);
        services.require(PlayerProfileService.class).save(playerId);
        return true;
    }

    @Override protected void onStop(final ServiceRegistry services) {
        services.find(FloorOneWorldService.class).ifPresent(FloorOneWorldService::stop);
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
}
