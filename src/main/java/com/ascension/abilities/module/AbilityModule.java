package com.ascension.abilities.module;

import com.ascension.abilities.command.AbilityCommand;
import com.ascension.abilities.platform.BukkitAbilityRuntimeGateway;
import com.ascension.abilities.service.AbilityBehaviorRegistry;
import com.ascension.abilities.service.AbilityExecutorRegistry;
import com.ascension.abilities.service.AbilityRuntimeGateway;
import com.ascension.abilities.service.AbilityService;
import com.ascension.abilities.service.CooldownTracker;
import com.ascension.abilities.service.DataDrivenAbilityExecutor;
import com.ascension.abilities.service.DefaultAbilityService;
import com.ascension.abilities.service.ResourceGateway;
import com.ascension.combat.service.CombatService;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.effects.service.EffectService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.service.AttributeService;
import java.util.Set;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/** Boots the data-driven ability execution layer. */
public final class AbilityModule extends AbstractModule {

    @Override
    public String id() { return "abilities"; }

    @Override
    public Set<String> dependencies() {
        return Set.of("assets", "registry", "session", "stats", "effects", "combat");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registryHub = services.require(RegistryHub.class);
        final AbilityExecutorRegistry executors = new AbilityExecutorRegistry();
        final AbilityBehaviorRegistry behaviors = new AbilityBehaviorRegistry();
        final AbilityRuntimeGateway runtime = new BukkitAbilityRuntimeGateway(
            services.require(PlayerSessionManager.class),
            services.require(AttributeService.class),
            services.require(CombatService.class),
            services.require(EffectService.class)
        );
        final DataDrivenAbilityExecutor dataDriven = new DataDrivenAbilityExecutor(runtime);
        behaviors.register("physical_attack", dataDriven);
        behaviors.register("area_physical_attack", dataDriven);
        behaviors.register("apply_effect", dataDriven);

        final DefaultAbilityService abilityService = new DefaultAbilityService(
            registryHub.require(AscensionRegistries.ABILITY_DEFINITIONS),
            executors,
            behaviors,
            ResourceGateway.noOp(),
            new CooldownTracker()
        );
        services.register(AbilityExecutorRegistry.class, executors);
        services.register(AbilityBehaviorRegistry.class, behaviors);
        services.register(AbilityRuntimeGateway.class, runtime);
        services.register(AbilityService.class, abilityService);
        registerCommand(services.require(JavaPlugin.class), abilityService);
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        if (services.has(AbilityService.class) && services.require(AbilityService.class) instanceof DefaultAbilityService abilityService) {
            for (final var session : services.require(PlayerSessionManager.class).sessions()) {
                abilityService.clearRuntime(session.uniqueId());
            }
        }
    }

    private static void registerCommand(final JavaPlugin plugin, final AbilityService abilityService) {
        final var command = plugin.getCommand("asc");
        if (command == null) return;
        final CommandExecutor previous = command.getExecutor();
        final AbilityCommand abilityCommand = new AbilityCommand(abilityService);
        command.setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd,
                                      @NotNull final String label, @NotNull final String[] args) {
                if (args.length > 0 && args[0].equalsIgnoreCase("cast")) {
                    return abilityCommand.onCommand(sender, cmd, label, args);
                }
                return previous != null && previous.onCommand(sender, cmd, label, args);
            }
        });
    }
}
