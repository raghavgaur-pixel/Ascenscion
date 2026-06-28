package com.ascension.equipment.debug;

import com.ascension.equipment.model.EquipmentSlot;
import com.ascension.equipment.model.EquipmentSnapshot;
import com.ascension.equipment.service.EquipmentService;
import com.ascension.session.model.PlayerSession;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.stats.attribute.AttributeValueSnapshot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EquipmentDebugCommands implements CommandExecutor {

    private final PlayerSessionManager sessionManager;
    private final EquipmentService equipmentService;

    public EquipmentDebugCommands(final PlayerSessionManager sessionManager, final EquipmentService equipmentService) {
        this.sessionManager = sessionManager;
        this.equipmentService = equipmentService;
    }

    @Override
    public boolean onCommand(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String label,
        @NotNull final String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        final PlayerSession session = this.sessionManager.session(player.getUniqueId()).orElse(null);
        if (session == null) {
            sender.sendMessage("No active session.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("/asc equipment|stats|debug");
            return true;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "equipment" -> {
                final EquipmentSnapshot snapshot = this.equipmentService.container(session).snapshot();
                sender.sendMessage("--- Equipment ---");
                for (final EquipmentSlot slot : EquipmentSlot.values()) {
                    snapshot.item(slot).ifPresent(item ->
                        sender.sendMessage(slot.name() + ": " + item.definitionId().toString())
                    );
                }
            }
            case "stats" -> {
                sender.sendMessage("--- Stats ---");
                for (final AttributeValueSnapshot attr : session.attributes().snapshot().values().values()) {
                    sender.sendMessage(attr.statId().toString() + ": " + attr.finalValue());
                }
            }
            case "debug" -> sender.sendMessage("Equipment Engine active.");
            default -> sender.sendMessage("Unknown subcommand.");
        }

        return true;
    }
}
