package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamShifter;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import com.andrewparry.seamlessplanet.wrap.SeamWrapHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
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

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void seamless$initWrap(CallbackInfo ci) {
        var wrap = SeamWrap.of((ServerLevel) (Object) this);
        ((SeamWrapHolder) blockTicks).seamless$setWrap(wrap);
        ((SeamWrapHolder) fluidTicks).seamless$setWrap(wrap);
        ((SeamWrapHolder) ((PersistentEntitySectionManagerAccessor) entityManager).seamless$sectionStorage()).seamless$setWrap(wrap);
    }

    /** After an entity (and its passengers) finished its tick, move it back if it crossed a seam. */
    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void seamless$shiftAfterTick(Entity entity, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of((ServerLevel) (Object) this);
        if (wrap != null && !entity.isRemoved()) {
            SeamShifter.shiftIntoRange(entity, wrap);
        }
    }
}
