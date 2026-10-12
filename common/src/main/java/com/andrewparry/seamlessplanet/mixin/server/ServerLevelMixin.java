package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import com.andrewparry.seamlessplanet.wrap.SeamWrapHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    @Final
    private LevelTicks<Block> blockTicks;

    @Shadow
    @Final
    private LevelTicks<Fluid> fluidTicks;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void seamless$initTickWrap(CallbackInfo ci) {
        var wrap = SeamWrap.of((ServerLevel) (Object) this);
        ((SeamWrapHolder) blockTicks).seamless$setWrap(wrap);
        ((SeamWrapHolder) fluidTicks).seamless$setWrap(wrap);
    }
}
