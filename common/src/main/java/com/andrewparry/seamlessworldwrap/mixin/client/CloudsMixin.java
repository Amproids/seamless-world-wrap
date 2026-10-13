package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Clouds are drawn as if the camera had never jumped across a seam, so they don't snap. */
@Mixin(LevelRenderer.class)
public abstract class CloudsMixin {

    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double seamless$cloudX(double camX) {
        return camX + ClientWrapState.cloudShiftX();
    }

    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double seamless$cloudZ(double camZ) {
        return camZ + ClientWrapState.cloudShiftZ();
    }
}
