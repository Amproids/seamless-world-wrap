package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.worldgen.Essentials;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After a chunk's normal decoration, small worlds get their guaranteed essentials. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void seamless$essentials(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        Essentials.decorate(level, chunk);
    }
}
