package com.ascension.equipment.module;

import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.equipment.debug.EquipmentDebugCommands;
import com.ascension.equipment.event.ItemEquippedEvent;
import com.ascension.equipment.event.ItemUnequippedEvent;
import com.ascension.equipment.integration.StatEquipmentListener;
import com.ascension.equipment.rule.EquipmentRuleRegistry;
import com.ascension.equipment.service.DefaultEquipmentService;
import com.ascension.equipment.service.EquipmentService;
import com.ascension.events.EventBus;
import com.ascension.events.EventPriority;
import com.ascension.items.service.ItemService;
import com.ascension.session.service.PlayerSessionManager;
import java.util.Set;
import org.bukkit.plugin.java.JavaPlugin;

public final class EquipmentModule extends AbstractModule {

    @Override
    public String id() {
        return "equipment";
    }

    @Override
    public Set<String> dependencies() {
        return Set.of("runtime-engine", "session", "items", "stats");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final EquipmentRuleRegistry ruleRegistry = new EquipmentRuleRegistry();
        final EventBus eventBus = services.require(EventBus.class);
        final DefaultEquipmentService equipmentService = new DefaultEquipmentService(ruleRegistry, eventBus);

        services.register(EquipmentRuleRegistry.class, ruleRegistry);
        services.register(EquipmentService.class, equipmentService);

        final StatEquipmentListener statIntegration = new StatEquipmentListener(services.require(ItemService.class));
        eventBus.subscribe("equipment", ItemEquippedEvent.class, EventPriority.NORMAL, false, statIntegration.onEquip());
        eventBus.subscribe("equipment", ItemUnequippedEvent.class, EventPriority.NORMAL, false, statIntegration.onUnequip());

        final JavaPlugin plugin = services.require(JavaPlugin.class);
        final org.bukkit.command.PluginCommand command = plugin.getCommand("asc");
        if (command != null) {
            command.setExecutor(new EquipmentDebugCommands(services.require(PlayerSessionManager.class), equipmentService));
        }
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.require(EventBus.class).unsubscribeOwner("equipment");
    }
}
