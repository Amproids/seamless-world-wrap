package com.andrewparry.seamlessworldwrap.client;

import com.andrewparry.seamlessworldwrap.WorldWrapOptions;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.network.chat.Component;

/** The choices on the World Wrap tab while a world is being set up. */
public final class WorldWrapDraft {

    public enum EndMode {
        SAME("same"), CUSTOM("custom"), OFF("off");

        private final String key;

        EndMode(String key) {
            this.key = key;
        }

        public Component label() {
            return Component.translatable("seamless_world_wrap.end_mode." + key);
        }
    }

    public static final class Axes {
        public boolean wrapsX = true;
        public boolean wrapsZ = true;
        public int widthChunks = 64;
        public int lengthChunks = 64;

        WrapSettings toSettings() {
            return wrapsX || wrapsZ ? WrapSettings.of(wrapsX, wrapsZ, widthChunks, lengthChunks) : null;
        }
    }

    private static WorldWrapDraft current = new WorldWrapDraft();

    public final Axes overworld = new Axes();
    public final Axes end = new Axes();
    public EndMode endMode = EndMode.SAME;
    public boolean startInForest = true;

    public static WorldWrapDraft current() {
        return current;
    }

    public static void reset() {
        current = new WorldWrapDraft();
    }

    public WorldWrapOptions toOptions() {
        WrapSettings overworldSettings = overworld.toSettings();
        WrapSettings endSettings = switch (endMode) {
            case SAME -> overworldSettings;
            case CUSTOM -> end.toSettings();
            case OFF -> null;
        };
        return new WorldWrapOptions(overworldSettings, endSettings, startInForest);
    }
}
