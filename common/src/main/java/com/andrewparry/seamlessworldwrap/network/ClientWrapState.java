package com.andrewparry.seamlessworldwrap.network;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** What the server told this client about wrapping. Empty until (unless) the server sends it. */
public final class ClientWrapState {

    private static volatile Map<ResourceLocation, WrapSettings> dimensions = Map.of();

    private ClientWrapState() {
    }

    @Nullable
    public static WrapSettings forDimension(ResourceKey<Level> dimension) {
        return dimensions.get(dimension.location());
    }

    public static void apply(WrapSyncPayload payload) {
        dimensions = Map.copyOf(payload.dimensions());
    }

    public static void clear() {
        dimensions = Map.of();
    }
}
