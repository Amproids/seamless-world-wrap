package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.server.level.ServerLevel;

/**
 * Startup waits until a square of spawn chunks is loaded. A world narrower than that square has
 * fewer canonical chunks than the square counts, so wait for the ones that actually exist.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract ServerLevel overworld();

    @ModifyVariable(method = "prepareLevels", at = @At("STORE"), ordinal = 1)
    private int seamless$spawnChunksThatExist(int expected) {
        WrapSettings wrap = SeamWrap.of(overworld());
        if (wrap == null || expected <= 0) {
            return expected;
        }
        int diameter = (int) Math.round(Math.sqrt(expected));
        int wide = Math.min(diameter, wrap.widthChunks());
        int tall = wrap.shape().wrapsZ() ? Math.min(diameter, wrap.heightChunks()) : diameter;
        return wide * tall;
    }
}
