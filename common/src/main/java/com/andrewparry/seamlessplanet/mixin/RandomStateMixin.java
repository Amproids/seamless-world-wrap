package com.andrewparry.seamlessplanet.mixin;

import com.andrewparry.seamlessplanet.SeamlessPlanet;
import com.andrewparry.seamlessplanet.worldgen.SeamWrapVisitor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Right after vanilla wires real noise instances into the router, wrap its noise leaves so the
 * terrain repeats and blends across the seams. The climate sampler (biomes) is built from this
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
    private NoiseRouter seamless_planet$wrapRouter(NoiseRouter router, DensityFunction.Visitor visitor, Operation<NoiseRouter> original) {
        NoiseRouter wired = original.call(router, visitor);
        return wired.mapAll(new SeamWrapVisitor(SeamlessPlanet.settings()));
    }
}
