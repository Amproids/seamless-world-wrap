package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Terrain is shaped around structures (flattened under houses, filled under paths). Pieces just
 * across a seam are copied over, moved by one world size, so the ground fits them on this side too.
 */
@Mixin(Beardifier.class)
public abstract class BeardifierMixin {

    @Shadow
    private ObjectListIterator<Beardifier.Rigid> pieceIterator;

    @Shadow
    private ObjectListIterator<JigsawJunction> junctionIterator;

    @Inject(method = "forStructuresInChunk", at = @At("RETURN"), cancellable = true)
    private static void seamless$acrossSeams(StructureManager structureManager, ChunkPos chunkPos, CallbackInfoReturnable<Beardifier> cir) {
        LevelAccessor level = ((StructureManagerAccessor) structureManager).seamless$level();
        WrapSettings wrap = SeamWrap.of(level);
        if (wrap == null) {
            return;
        }
        ObjectList<Beardifier.Rigid> pieces = new ObjectArrayList<>();
        ObjectList<JigsawJunction> junctions = new ObjectArrayList<>();
        BeardifierMixin vanilla = (BeardifierMixin) (Object) cir.getReturnValue();
        vanilla.pieceIterator.forEachRemaining(pieces::add);
        vanilla.junctionIterator.forEachRemaining(junctions::add);
        int added = 0;
        for (int[] shift : SeamWrap.aliasShifts(wrap)) {
            ChunkPos alias = new ChunkPos(chunkPos.x + shift[0] / 16, chunkPos.z + shift[1] / 16);
            int ax = alias.getMinBlockX();
            int az = alias.getMinBlockZ();
            for (var start : structureManager.startsForStructure(chunkPos, s -> s.terrainAdaptation() != TerrainAdjustment.NONE)) {
                TerrainAdjustment adjustment = start.getStructure().terrainAdaptation();
                for (StructurePiece piece : start.getPieces()) {
                    if (!piece.isCloseToChunk(alias, 12) || piece.isCloseToChunk(chunkPos, 12)) {
                        continue;
                    }
                    if (piece instanceof PoolElementStructurePiece pool) {
                        if (pool.getElement().getProjection() == StructureTemplatePool.Projection.RIGID) {
                            pieces.add(new Beardifier.Rigid(pool.getBoundingBox().moved(-shift[0], 0, -shift[1]), adjustment, pool.getGroundLevelDelta()));
                            added++;
                        }
                        for (JigsawJunction junction : pool.getJunctions()) {
                            int jx = junction.getSourceX();
                            int jz = junction.getSourceZ();
                            if (jx > ax - 12 && jz > az - 12 && jx < ax + 15 + 12 && jz < az + 15 + 12) {
                                junctions.add(new JigsawJunction(jx - shift[0], junction.getSourceGroundY(), jz - shift[1],
                                        junction.getDeltaY(), junction.getDestProjection()));
                                added++;
                            }
                        }
                    } else {
                        pieces.add(new Beardifier.Rigid(piece.getBoundingBox().moved(-shift[0], 0, -shift[1]), adjustment, 0));
                        added++;
                    }
                }
            }
        }
        cir.setReturnValue(new Beardifier(pieces.iterator(), junctions.iterator()));
    }
}
