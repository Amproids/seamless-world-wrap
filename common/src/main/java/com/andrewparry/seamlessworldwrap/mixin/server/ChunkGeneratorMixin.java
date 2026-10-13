package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.worldgen.Essentials;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a chunk's normal decoration, small worlds get their guaranteed essentials. Structure
 * references also look across seams: a structure that started on the other side of an edge and
 * reaches over it is recorded in the chunks it reaches, so its pieces get built there.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Redirect(method = "createReferences",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;intersects(IIII)Z"))
    private boolean seamless$referenceAcrossSeams(BoundingBox structure, int minX, int minZ, int maxX, int maxZ,
                                                  WorldGenLevel level, StructureManager structures, ChunkAccess chunk) {
        if (structure.intersects(minX, minZ, maxX, maxZ)) {
            return true;
        }
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return false;
        }
        for (int[] shift : SeamWrap.aliasShifts(wrap)) {
            if (structure.intersects(minX + shift[0], minZ + shift[1], maxX + shift[0], maxZ + shift[1])) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void seamless$essentials(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        Essentials.decorate(level, chunk);
    }
}
