package com.ascension.combat.module;

import com.ascension.combat.debug.CombatDebugCommand;
import com.ascension.combat.service.CombatService;
import com.ascension.combat.service.DefaultCombatService;
import com.ascension.combat.service.FriendlyFireValidator;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.events.EventBus;

import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class CombatModule extends AbstractModule {

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public Set<String> dependencies() {
        return Set.of("runtime-engine", "equipment", "stats");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final EventBus eventBus = services.require(EventBus.class);
        final DefaultCombatService combatService = new DefaultCombatService(eventBus);

        // Register default validators
        combatService.registerValidator(new FriendlyFireValidator());

        services.register(CombatService.class, combatService);

        final JavaPlugin plugin = services.require(JavaPlugin.class);
        final org.bukkit.command.PluginCommand command = plugin.getCommand("asc");
        if (command != null) {
            final CommandExecutor previousExecutor = command.getExecutor();
            final CombatDebugCommand combatDebugCommand = new CombatDebugCommand(combatService);

            eventBus.subscribe("combat", com.ascension.combat.event.DamageCalculatedEvent.class, com.ascension.events.EventPriority.MONITOR, false, combatDebugCommand.listener());

            // Create a wrapper that acts as a router to prevent overwriting existing commands
            command.setExecutor(new CommandExecutor() {
                @Override
                public boolean onCommand(
                    @NotNull final CommandSender sender,
                    @NotNull final Command cmd,
                    @NotNull final String label,
                    @NotNull final String[] args
                ) {
                    if (args.length > 0 && isCombatSubCommand(args[0])) {
                        return combatDebugCommand.onCommand(sender, cmd, label, args);
                    }
                    if (previousExecutor != null) {
                        return previousExecutor.onCommand(sender, cmd, label, args);
                    }
                    return false;
                }

                private boolean isCombatSubCommand(final String arg) {
                    final String lower = arg.toLowerCase(java.util.Locale.ROOT);
                    return lower.equals("combat");
                }
            });
        }
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(EventBus.class).unsubscribeOwner("combat");
    }
}
