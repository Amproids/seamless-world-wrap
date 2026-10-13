package com.andrewparry.seamlessworldwrap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * The running server's wrap settings, per dimension. Loaded from the world folder before any level
 * is created; a new world takes the choices from the Create World screen (or, on a dedicated
 * server, the config file); a world made before the mod was added never wraps.
 */
public final class WorldWrapState {

    public static final String FILE_NAME = SeamlessWorldWrap.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile Map<ResourceKey<Level>, WrapSettings> active = Map.of();
    private static volatile WorldWrapOptions options = WorldWrapOptions.NONE;
    /** Set by the Create World screen when a world is created; consumed by that world's server. */
    @Nullable
    private static volatile WorldWrapOptions pendingNewWorld;

    private WorldWrapState() {
    }

    @Nullable
    public static WrapSettings forDimension(ResourceKey<Level> dimension) {
        return active.get(dimension);
    }

    public static WorldWrapOptions options() {
        return options;
    }

    public static void setPendingNewWorld(@Nullable WorldWrapOptions pending) {
        pendingNewWorld = pending;
    }

    /**
     * @param worldDir  the world's folder
     * @param brandNew  true if the world has no saved data yet
     */
    public static void load(Path worldDir, boolean brandNew) {
        Path file = worldDir.resolve(FILE_NAME);
        WorldWrapOptions loaded;
        if (Files.exists(file)) {
            loaded = read(file);
        } else {
            WorldWrapOptions pending = pendingNewWorld;
            if (pending != null) {
                loaded = pending;
            } else if (brandNew) {
                loaded = WrapConfig.loadDefaults(SeamlessWorldWrap.configFile());
            } else {
                loaded = WorldWrapOptions.NONE; // existing world from before the mod: leave it alone
            }
            write(file, loaded);
        }
        pendingNewWorld = null;
        options = loaded;
        active = Map.copyOf(loaded.byDimension());
        SeamlessWorldWrap.LOG.info("World wrap for this world: {}", active.isEmpty() ? "off" : active);
    }

    public static void clear() {
        active = Map.of();
        options = WorldWrapOptions.NONE;
    }

    private static WorldWrapOptions read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return WorldWrapOptions.fromJson(GSON.fromJson(reader, JsonObject.class));
        } catch (IOException | RuntimeException e) {
            SeamlessWorldWrap.LOG.error("Couldn't read {}; world wrap is off for this session", file, e);
            return WorldWrapOptions.NONE;
        }
    }

    private static void write(Path file, WorldWrapOptions value) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(value.toJson(), writer);
            }
        } catch (IOException e) {
            SeamlessWorldWrap.LOG.error("Couldn't save {}", file, e);
        }
    }
}
