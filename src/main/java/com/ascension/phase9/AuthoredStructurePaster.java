package com.ascension.phase9;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/** Pastes authored structures through WorldEdit when it is installed on the server. */
public final class AuthoredStructurePaster {
    private static final String PASTE_MARKER = ".floor_001_first_gate_pasted_v1";
    private static final int FIRST_GATE_X = -50;
    private static final int FIRST_GATE_Y = 70;
    private static final int FIRST_GATE_Z = 690;

    private AuthoredStructurePaster() {
    }

    public static void apply(final JavaPlugin plugin) {
        final Path marker = plugin.getDataFolder().toPath().resolve(PASTE_MARKER);
        if (Files.exists(marker)) return;

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            plugin.getLogger().warning("WorldEdit is not installed; skipping authored First Gate schematic paste.");
            return;
        }

        final World world = Bukkit.getWorld(FloorOneWorldService.WORLD_NAME);
        if (world == null) {
            plugin.getLogger().warning("Floor 1 world is not loaded; skipping authored First Gate schematic paste.");
            return;
        }

        final Path schematic = plugin.getDataFolder().toPath()
            .resolve("floor1-assets")
            .resolve("medieval-town-collection-1-castle.schem");
        if (!Files.exists(schematic)) {
            plugin.getLogger().warning("Missing First Gate schematic: " + schematic);
            return;
        }

        try (InputStream input = Files.newInputStream(schematic)) {
            final ClipboardFormat format = ClipboardFormats.findByFile(schematic.toFile());
            if (format == null) {
                plugin.getLogger().warning("WorldEdit could not identify schematic format: " + schematic.getFileName());
                return;
            }
            final Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(input)) {
                clipboard = reader.read();
            }
            final com.sk89q.worldedit.world.World targetWorld = BukkitAdapter.adapt(world);
            try (var editSession = WorldEdit.getInstance().newEditSession(targetWorld)) {
                final Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(FIRST_GATE_X, FIRST_GATE_Y, FIRST_GATE_Z))
                    .ignoreAirBlocks(false)
                    .build();
                Operations.complete(operation);
            }
            Files.writeString(marker, "floor_001_first_gate_pasted_v1");
            plugin.getLogger().info("Pasted authored First Gate castle at " + FIRST_GATE_X + "," + FIRST_GATE_Y + "," + FIRST_GATE_Z);
        } catch (IOException | WorldEditException exception) {
            plugin.getLogger().warning("Unable to paste authored First Gate: " + exception.getMessage());
        }
    }
}
