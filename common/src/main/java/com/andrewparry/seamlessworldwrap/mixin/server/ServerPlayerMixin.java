package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.wrap.SeamPlayer;
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
    }
}
