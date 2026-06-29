package com.ascension.combat.debug;

import com.ascension.combat.event.DamageCalculatedEvent;
import com.ascension.combat.model.CombatContext;
import com.ascension.combat.service.CombatService;
import com.ascension.events.EventListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CombatDebugCommand implements CommandExecutor {

    private final CombatService combatService;
    private volatile CombatContext lastContext;

    public CombatDebugCommand(final CombatService combatService) {
        this.combatService = combatService;
    }

    public EventListener<DamageCalculatedEvent> listener() {
        return event -> this.lastContext = event.context();
    }

    @Override
    public boolean onCommand(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String label,
        @NotNull final String[] args
    ) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("/asc combat <context|damage>");
            return true;
        }

        final CombatContext context = this.lastContext;
        if (context == null) {
            sender.sendMessage("No combat context recorded yet.");
            return true;
        }

        switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "context" -> {
                sender.sendMessage("--- Last Combat Context ---");
                sender.sendMessage("Target: " + context.attackContext().target().entity().name());
                context.attackContext().attacker().ifPresent(attacker ->
                    sender.sendMessage("Attacker: " + attacker.name())
                );
                sender.sendMessage("Source: " + context.attackContext().damageContext().source().name());
                sender.sendMessage("Type: " + context.attackContext().damageContext().type().name());
                sender.sendMessage("Base Amount: " + context.attackContext().damageContext().baseAmount());
            }
            case "damage" -> {
                sender.sendMessage("--- Last Damage Result ---");
                sender.sendMessage("Final Damage: " + context.currentDamageResult().finalDamage());
                sender.sendMessage("Mitigated: " + context.currentDamageResult().mitigated());
                sender.sendMessage("Critical: " + context.currentDamageResult().isCritical());
            }
            default -> sender.sendMessage("Unknown combat debug subcommand.");
        }

        return true;
    }
}
