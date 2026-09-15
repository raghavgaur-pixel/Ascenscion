package com.ascension.phase9;

import com.ascension.assets.model.AssetId;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.session.service.PlayerSessionManager;
import com.ascension.tower.model.FloorId;
import com.ascension.tower.service.TowerService;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;

/** Development/vertical-slice command surface for Phase 9. */
public final class PhaseNineCommand implements CommandExecutor {

    private final QuestService quests;
    private final MobService mobs;
    private final PlayerSessionManager sessions;
    private final TowerService tower;

    public PhaseNineCommand(final QuestService quests, final MobService mobs,
                            final PlayerSessionManager sessions, final TowerService tower) {
        this.quests = Objects.requireNonNull(quests, "quests");
        this.mobs = Objects.requireNonNull(mobs, "mobs");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.tower = Objects.requireNonNull(tower, "tower");
    }

    public boolean handles(final String[] args) {
        return args.length > 0 && (args[0].equalsIgnoreCase("quest") || args[0].equalsIgnoreCase("mob") || args[0].equalsIgnoreCase("floor"));
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                             @NotNull final String label, @NotNull final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command requires a player.");
            return true;
        }
        if (args.length == 0) return false;
        return switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "quest" -> quest(player, args);
            case "mob" -> mob(player, args);
            case "floor" -> floor(player, args);
            default -> false;
        };
    }

    private boolean quest(final Player player, final String[] args) {
        if (args.length < 2) { player.sendMessage(Component.text("/asc quest <accept|list|talk|reach> ...")); return true; }
        final QuestService.Result result = switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "accept" -> args.length >= 3 ? this.quests.accept(player.getUniqueId(), args[2]) : QuestService.Result.rejected("Quest id required");
            case "talk" -> args.length >= 3 ? this.quests.talkToNpc(player.getUniqueId(), args[2]) : QuestService.Result.rejected("NPC id required");
            case "reach" -> args.length >= 3 ? this.quests.reachLocation(player.getUniqueId(), args[2]) : QuestService.Result.rejected("Location required");
            case "list" -> {
                player.sendMessage(Component.text("Active quests: " + String.join(", ", this.quests.active(player.getUniqueId()))));
                yield QuestService.Result.success();
            }
            default -> QuestService.Result.rejected("Unknown quest operation");
        };
        player.sendMessage(Component.text(result.status() == QuestService.Status.SUCCESS ? "Done." : result.reason()));
        return true;
    }

    private boolean mob(final Player player, final String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("spawn")) {
            player.sendMessage(Component.text("/asc mob spawn <mob-id|boss-id>"));
            return true;
        }
        final AssetId id;
        try { id = AssetId.parse(args[2]); }
        catch (IllegalArgumentException exception) { player.sendMessage(Component.text("Invalid asset id.")); return true; }
        if (this.mobs.spawnMob(id, player.getLocation()).isEmpty() && this.mobs.spawnBoss(id, player.getLocation()).isEmpty()) {
            player.sendMessage(Component.text("Unknown mob or boss: " + id));
        } else {
            player.sendMessage(Component.text("Spawned " + id));
        }
        return true;
    }

    private boolean floor(final Player player, final String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("unlock")) {
            player.sendMessage(Component.text("/asc floor unlock <floor-id>"));
            return true;
        }
        final var session = this.sessions.session(player.getUniqueId()).flatMap(s -> s.profile()).orElse(null);
        if (session == null) { player.sendMessage(Component.text("Profile is not ready.")); return true; }
        try {
            final AssetId id = AssetId.parse(args[2]);
            final var result = this.tower.unlock(session.components().find("unlocked_floors").map(UnlockedFloorsProfileComponent.class::cast).orElseThrow(), new FloorId(id.toString()));
            player.sendMessage(Component.text("Floor " + id + ": " + result.status()));
        } catch (RuntimeException exception) {
            player.sendMessage(Component.text("Unable to unlock floor: " + exception.getMessage()));
        }
        return true;
    }
}
