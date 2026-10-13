package com.andrewparry.seamlessworldwrap.network;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.WorldWrapState;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Tells a client how each dimension wraps, so it can draw entity copies and smooth seam crossings.
 * Optional: clients without the mod never receive it and simply see an endless world.
 */
public record WrapSyncPayload(Map<ResourceLocation, WrapSettings> dimensions) implements CustomPacketPayload {

    public static final Type<WrapSyncPayload> TYPE = new Type<>(SeamlessWorldWrap.id("wrap_settings"));
    public static final StreamCodec<FriendlyByteBuf, WrapSyncPayload> CODEC =
            CustomPacketPayload.codec(WrapSyncPayload::write, WrapSyncPayload::read);

    public static WrapSyncPayload current() {
        Map<ResourceLocation, WrapSettings> map = new HashMap<>();
        for (ResourceKey<Level> key : new ResourceKey[]{Level.OVERWORLD, Level.NETHER, Level.END}) {
            WrapSettings settings = WorldWrapState.forDimension(key);
            if (settings != null) {
                map.put(key.location(), settings);
            }
        }
        return new WrapSyncPayload(map);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(dimensions.size());
        dimensions.forEach((id, s) -> {
            buf.writeResourceLocation(id);
            buf.writeBoolean(s.wrapsX());
            buf.writeBoolean(s.wrapsZ());
            buf.writeVarInt(s.widthChunks());
            buf.writeVarInt(s.heightChunks());
            buf.writeVarInt(s.blendWidth());
        });
    }

    private static WrapSyncPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<ResourceLocation, WrapSettings> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            map.put(id, new WrapSettings(buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new WrapSyncPayload(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
