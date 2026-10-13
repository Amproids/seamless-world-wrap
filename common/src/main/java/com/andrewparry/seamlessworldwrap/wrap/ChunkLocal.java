package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WorldWrapState;
import com.andrewparry.seamlessworldwrap.mixin.server.BlockEntityPositionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Blocks written at an alias (e.g. a village piece built across a seam) land in the real chunk,
 * but their block entities would be filed under the alias position and never found again. A chunk
 * only holds its own blocks, so a position handed to it is its own up to whole chunks: file block
 * entities under the chunk's own coordinates.
 */
public final class ChunkLocal {

    private ChunkLocal() {
    }

    public static BlockPos own(ChunkPos chunk, BlockPos pos) {
        if (!WorldWrapState.anyWrap()) {
            return pos;
        }
        int x = chunk.getMinBlockX() + (pos.getX() & 15);
        int z = chunk.getMinBlockZ() + (pos.getZ() & 15);
        return x == pos.getX() && z == pos.getZ() ? pos : new BlockPos(x, pos.getY(), z);
    }

    public static void ownTag(ChunkPos chunk, CompoundTag tag) {
        BlockPos pos = BlockEntity.getPosFromTag(tag);
        BlockPos own = own(chunk, pos);
        if (own != pos) {
            tag.putInt("x", own.getX());
            tag.putInt("z", own.getZ());
        }
    }

    public static void ownEntity(ChunkPos chunk, BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockPos own = own(chunk, pos);
        if (own != pos) {
            ((BlockEntityPositionAccessor) blockEntity).seamless$setWorldPosition(own);
        }
    }
}
