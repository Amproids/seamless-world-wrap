package com.andrewparry.seamlessworldwrap;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Reads (and on first run writes) the world shape and size from a properties file. */
public final class WrapConfig {

    private WrapConfig() {
    }

    public static WrapSettings load(Path file) {
        WrapSettings defaults = WrapSettings.DEFAULT;
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                props.load(reader);
            } catch (IOException e) {
                SeamlessWorldWrap.LOG.error("Couldn't read {}, using defaults", file, e);
                return defaults;
            }
        } else {
            write(file, defaults);
            return defaults;
        }
        try {
            WorldShape shape = WorldShape.valueOf(props.getProperty("shape", defaults.shape().name()).trim().toUpperCase(Locale.ROOT));
            int width = Integer.parseInt(props.getProperty("widthChunks", String.valueOf(defaults.widthChunks())).trim());
            int height = Integer.parseInt(props.getProperty("heightChunks", String.valueOf(defaults.heightChunks())).trim());
            int blend = Integer.parseInt(props.getProperty("blendWidth", String.valueOf(defaults.blendWidth())).trim());
            // Keep the blend inside half the world so small worlds work without editing it.
            blend = Math.min(blend, Math.min(width, height) * 8);
            return new WrapSettings(shape, width, height, blend);
        } catch (IllegalArgumentException e) {
            SeamlessWorldWrap.LOG.error("Invalid {} ({}), using defaults", file, e.getMessage());
            return defaults;
        }
    }

    private static void write(Path file, WrapSettings settings) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                writer.write("""
                        # Seamless World Wrap world settings. Applies to every world on this game or server.
                        # Changing these for an existing world moves its seams.
                        #
                        # shape: CYLINDER (east/west wrap) or TORUS (all four edges wrap)
                        # widthChunks / heightChunks: world size in chunks (16 blocks each), at least 4
                        # blendWidth: blocks before each seam where terrain fades across it (at most half the world)
                        """);
                writer.write("shape=" + settings.shape().name() + "\n");
                writer.write("widthChunks=" + settings.widthChunks() + "\n");
                writer.write("heightChunks=" + settings.heightChunks() + "\n");
                writer.write("blendWidth=" + settings.blendWidth() + "\n");
            }
        } catch (IOException e) {
            SeamlessWorldWrap.LOG.warn("Couldn't write default config {}", file, e);
        }
    }
}
