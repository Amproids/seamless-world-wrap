package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WorldWrapState;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    public abstract ServerLevel overworld();

    /** Every level's generator and chunk map read the wrap, so load it before any level exists. */
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void seamless$loadWrap(CallbackInfo ci) {
        boolean brandNew = !Files.exists(storageSource.getLevelPath(LevelResource.LEVEL_DATA_FILE));
        WorldWrapState.load(storageSource.getLevelPath(LevelResource.ROOT), brandNew);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void seamless$clearWrap(CallbackInfo ci) {
        WorldWrapState.clear();
    }

    /**
     * Startup waits until a square of spawn chunks is loaded. A world narrower than that square has
     * fewer canonical chunks than the square counts, so wait for the ones that actually exist.
     */
    @ModifyVariable(method = "prepareLevels", at = @At("STORE"), ordinal = 1)
    private int seamless$spawnChunksThatExist(int expected) {
        WrapSettings wrap = SeamWrap.of(overworld());
        if (wrap == null || expected <= 0) {
            return expected;
        }
        int diameter = (int) Math.round(Math.sqrt(expected));
        int wide = wrap.wrapsX() ? Math.min(diameter, wrap.widthChunks()) : diameter;
        int tall = wrap.wrapsZ() ? Math.min(diameter, wrap.heightChunks()) : diameter;
        return wide * tall;
    }
}
