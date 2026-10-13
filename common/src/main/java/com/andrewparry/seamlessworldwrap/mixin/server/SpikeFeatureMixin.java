package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.worldgen.SmallEnd;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Both world generation and the dragon fight (crystal respawns) get pillars from here. */
@Mixin(SpikeFeature.class)
public abstract class SpikeFeatureMixin {

    @Inject(method = "getSpikesForLevel", at = @At("RETURN"), cancellable = true)
    private static void seamless$fitPillars(WorldGenLevel level, CallbackInfoReturnable<List<SpikeFeature.EndSpike>> cir) {
        List<SpikeFeature.EndSpike> fitted = SmallEnd.fitPillars(level, cir.getReturnValue());
        if (fitted != null) {
            cir.setReturnValue(fitted);
        }
    }
}
