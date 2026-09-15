package com.ascension.abilities.command;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.service.AbilityService;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Development-facing ability trigger for validating the full runtime pipeline.
 * Usage: /asc cast <ability-id> [online-player-target]
 */
public final class AbilityCommand implements CommandExecutor {

    private final AbilityService abilities;

    public AbilityCommand(final AbilityService abilities) {
        this.abilities = Objects.requireNonNull(abilities, "abilities");
    }

    @Override
    public boolean onCommand(
        @NotNull final CommandSender sender,
        @NotNull final Command command,
        @NotNull final String label,
        @NotNull final String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can cast Ascension abilities.");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("cast")) {
            sender.sendMessage("Usage: /asc cast <ability-id> [player-target]");
            return true;
        }

        final String abilityId = args[1].contains(":")
            ? args[1]
            : "ascension:" + args[1].toLowerCase(Locale.ROOT);

        Optional<LivingEntity> target = Optional.empty();
        if (args.length >= 3) {
            target = Optional.ofNullable(Bukkit.getPlayerExact(args[2]));
            if (target.isEmpty()) {
                sender.sendMessage("That player is not online.");
                return true;
            }
        } else if (player.getTargetEntity(8) instanceof LivingEntity living) {
            target = Optional.of(living);
        }

        if (target.isEmpty()) {
            sender.sendMessage("Look at a living entity or provide an online player target.");
            return true;
        }

        final var result = this.abilities.execute(new AbilityRequest(
            player.getUniqueId(),
            abilityId,
            target.get().getUniqueId()
        ));
        sender.sendMessage(result.status() == com.ascension.abilities.model.AbilityResult.Status.SUCCESS
            ? "Ability executed."
            : "Ability rejected: " + result.reason());
        return true;
    }
}
