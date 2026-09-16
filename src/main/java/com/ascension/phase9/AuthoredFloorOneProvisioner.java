package com.ascension.phase9;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.bukkit.plugin.java.JavaPlugin;

/** Installs the authored Floor 1 world assembled from the supplied donor assets. */
public final class AuthoredFloorOneProvisioner {
    private static final String BUNDLE_NAME = "Ascension-Floor1-Authored-Assets-v1.zip";
    private static final String WORLD_PREFIX = "world/";
    private static final String SCHEMATIC_PREFIX = "first-gate/";
    private static final String OLD_BUILD_MARKER = ".floor_001_provisioned";
    private static final String OLD_PREMIUM_MARKER = ".floor_001_premium";
    private static final String AUTHORED_MARKER = ".floor_001_authored_v1";

    private AuthoredFloorOneProvisioner() {
    }

    public static void install(final JavaPlugin plugin) {
        final Path data = plugin.getDataFolder().toPath();
        final Path worldDir = plugin.getServer().getWorldContainer().toPath().resolve(FloorOneWorldService.WORLD_NAME);
        final Path authoredMarker = data.resolve(AUTHORED_MARKER);
        if (Files.exists(authoredMarker) && Files.exists(worldDir.resolve("level.dat"))) {
            return;
        }

        final Path bundle = data.resolve(BUNDLE_NAME);
        if (!Files.exists(bundle)) {
            throw new IllegalStateException(
                "Missing " + BUNDLE_NAME + " in " + data + ". Download the Ascension Floor 1 authored asset bundle and place it there."
            );
        }

        final Path oldMarker = data.resolve(OLD_BUILD_MARKER);
        if (Files.exists(worldDir)) {
            if (!Files.exists(oldMarker)) {
                throw new IllegalStateException(
                    "Floor 1 world already exists at " + worldDir
                        + " without an Ascension build marker. Back it up or remove it before installing the authored Floor 1 world."
                );
            }
            deleteTree(worldDir);
        }

        try (ZipFile zip = new ZipFile(bundle.toFile())) {
            extractPrefix(zip, WORLD_PREFIX, worldDir);
            extractPrefix(zip, SCHEMATIC_PREFIX, data.resolve("floor1-assets"));
            Files.writeString(authoredMarker, "floor_001_authored_v1");
            Files.writeString(oldMarker, "floor_001_world_v8");
            Files.deleteIfExists(data.resolve(OLD_PREMIUM_MARKER));
            Files.deleteIfExists(data.resolve(".floor_001_premium"));
            plugin.getLogger().info("Installed authored Floor 1 world from " + bundle.getFileName());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to install authored Floor 1 assets", exception);
        }
    }

    private static void extractPrefix(final ZipFile zip, final String prefix, final Path destination) throws IOException {
        Files.createDirectories(destination);
        final Path normalizedRoot = destination.toAbsolutePath().normalize();
        boolean extracted = false;
        final var entries = zip.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)) continue;
            final String relativeName = entry.getName().substring(prefix.length());
            if (relativeName.isBlank()) continue;
            final Path output = destination.resolve(relativeName).normalize();
            if (!output.toAbsolutePath().startsWith(normalizedRoot)) {
                throw new IOException("Unsafe asset path in bundle: " + entry.getName());
            }
            Files.createDirectories(output.getParent());
            try (InputStream input = zip.getInputStream(entry)) {
                Files.copy(input, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            extracted = true;
        }
        if (!extracted) throw new IOException("Bundle contains no entries under " + prefix);
    }

    private static void deleteTree(final Path root) {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to replace legacy Floor 1 world", exception);
        }
    }
}
