package com.andrewparry.seamlessworldwrap;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Defaults for new worlds created without the Create World screen (dedicated servers). Each world
 * saves its own settings when it's created, so editing this never changes existing worlds.
 */
public final class WrapConfig {

    private WrapConfig() {
    }

    public static WorldWrapOptions loadDefaults(Path file) {
        if (!Files.exists(file)) {
            write(file);
            return WorldWrapOptions.defaults();
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            SeamlessWorldWrap.LOG.error("Couldn't read {}, using defaults", file, e);
            return WorldWrapOptions.defaults();
        }
        try {
            WrapSettings overworld = settings(props, "", null);
            String endMode = props.getProperty("end", "same").trim().toLowerCase(Locale.ROOT);
            WrapSettings end = switch (endMode) {
                case "off" -> null;
                case "custom" -> settings(props, "end.", overworld);
                default -> overworld;
            };
            return new WorldWrapOptions(overworld, end);
        } catch (IllegalArgumentException e) {
            SeamlessWorldWrap.LOG.error("Invalid {} ({}), using defaults", file, e.getMessage());
            return WorldWrapOptions.defaults();
        }
    }

    /** Null if neither axis wraps. */
    private static WrapSettings settings(Properties props, String prefix, WrapSettings fallback) {
        boolean x = bool(props, prefix + "wrapEastWest", fallback == null || fallback.wrapsX());
        boolean z = bool(props, prefix + "wrapNorthSouth", fallback == null || fallback.wrapsZ());
        if (!x && !z) {
            return null;
        }
        int width = integer(props, prefix + "widthChunks", fallback == null ? 64 : fallback.widthChunks());
        int length = integer(props, prefix + "lengthChunks", fallback == null ? 64 : fallback.heightChunks());
        String blend = props.getProperty(prefix + "blendWidth", "auto").trim();
        if (blend.equalsIgnoreCase("auto")) {
            return WrapSettings.of(x, z, width, length);
        }
        int b = Math.min(Integer.parseInt(blend), Math.min(width, length) * 8);
        return new WrapSettings(x, z, width, length, b);
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    private static int integer(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Integer.parseInt(value.trim());
    }

    private static void write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                writer.write("""
                        # Seamless World Wrap: defaults for NEW worlds created without the Create World
                        # screen (e.g. dedicated servers). Each world saves its own settings in
                        # <world>/seamless_world_wrap.json when it's created; editing this file never
                        # changes existing worlds. Worlds made before the mod was installed don't wrap.
                        #
                        # Sizes are in chunks (16 blocks), at least 4. The world is centered on 0,0.
                        # The Nether always wraps at 1/8 of the Overworld size so portals line up.
                        wrapEastWest=true
                        wrapNorthSouth=true
                        widthChunks=64
                        lengthChunks=64
                        # Blocks before each edge where terrain fades across it, or "auto".
                        blendWidth=auto

                        # The End: same (as the Overworld), custom (end.* below), or off.
                        end=same
                        #end.wrapEastWest=true
                        #end.wrapNorthSouth=true
                        #end.widthChunks=128
                        #end.lengthChunks=128
                        """);
            }
        } catch (IOException e) {
            SeamlessWorldWrap.LOG.warn("Couldn't write default config {}", file, e);
        }
    }
}
