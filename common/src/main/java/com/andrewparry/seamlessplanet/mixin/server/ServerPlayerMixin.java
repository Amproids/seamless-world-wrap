package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.wrap.SeamPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements SeamPlayer {

    @Unique
    private int seamless$offsetX;

    @Unique
    private int seamless$offsetZ;

    @Unique
    private final Int2ObjectMap<double[]> seamless$entityClientPos = new Int2ObjectOpenHashMap<>();

    @Override
    public Int2ObjectMap<double[]> seamless$entityClientPos() {
        return seamless$entityClientPos;
    }

    @Override
    public int seamless$offsetX() {
        return seamless$offsetX;
    }

    @Override
    public int seamless$offsetZ() {
        return seamless$offsetZ;
    }

    @Override
    public void seamless$setOffset(int x, int z) {
        this.seamless$offsetX = x;
        this.seamless$offsetZ = z;
    }

    /** A new level means a fresh client world, so client and canonical coordinates match again. */
    @Inject(method = "setServerLevel", at = @At("HEAD"))
    private void seamless$resetOffset(ServerLevel level, CallbackInfo ci) {
        seamless$setOffset(0, 0);
        seamless$entityClientPos.clear();
    }
}
