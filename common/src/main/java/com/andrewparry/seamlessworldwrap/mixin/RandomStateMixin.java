package com.andrewparry.seamlessworldwrap.mixin;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.andrewparry.seamlessworldwrap.worldgen.SeamWrapVisitor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Right after vanilla wires real noise instances into the router, wrap its noise leaves so the
 * terrain repeats and blends across the seams. Only for the level being created (see ChunkMapMixin). The climate sampler (biomes) is built from this
 * same router afterwards, so biomes wrap too.
 */
@Mixin(RandomState.class)
public class RandomStateMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
            )
    )
    private NoiseRouter seamless_world_wrap$wrapRouter(NoiseRouter router, DensityFunction.Visitor visitor, Operation<NoiseRouter> original) {
        NoiseRouter wired = original.call(router, visitor);
        WrapSettings wrap = SeamWrap.CREATING.get();
        int[] offset = SeamWrap.CREATING_OFFSET.get();
        return wrap == null ? wired : wired.mapAll(new SeamWrapVisitor(wrap, offset == null ? 0 : offset[0], offset == null ? 0 : offset[1]));
    }
}
