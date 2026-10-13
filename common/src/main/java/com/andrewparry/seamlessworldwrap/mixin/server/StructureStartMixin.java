package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A chunk just across a seam from a structure builds its pieces at the alias of itself that lies
 * next to the structure (one world size over). The blocks land in this chunk, because every
 * world-generation lookup at an alias resolves to the real chunk.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {

    @Shadow
    public abstract BoundingBox getBoundingBox();

    @Shadow
    public abstract void placeInChunk(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                      RandomSource random, BoundingBox box, ChunkPos chunkPos);

    @Inject(method = "placeInChunk", at = @At("HEAD"), cancellable = true)
    private void seamless$placeAcrossSeams(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                                           RandomSource random, BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        WrapSettings wrap = SeamWrap.of(level);
        BoundingBox structure = getBoundingBox();
        if (wrap == null || structure.intersects(box)) {
            return;
        }
        ci.cancel();
        for (int[] shift : SeamWrap.aliasShifts(wrap)) {
            BoundingBox alias = box.moved(shift[0], 0, shift[1]);
            if (structure.intersects(alias)) {
                placeInChunk(level, structureManager, generator, random, alias,
                        new ChunkPos(chunkPos.x + shift[0] / 16, chunkPos.z + shift[1] / 16));
            }
        }
    }
}
