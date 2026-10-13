package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrapHolder;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ticket levels spread to neighbors across the seam, so chunks are only ever loaded at canonical positions. */
@Mixin(ChunkTracker.class)
public abstract class ChunkTrackerMixin implements SeamWrapHolder {

    @Unique
    private WrapSettings seamless$wrap;

    @Override
    public WrapSettings seamless$wrap() {
        return seamless$wrap;
    }

    @Override
    public void seamless$setWrap(WrapSettings wrap) {
        this.seamless$wrap = wrap;
    }

    @Redirect(method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;asLong(II)J"))
    private long seamless$wrapNeighbor(int x, int z) {
        long pos = ChunkPos.asLong(x, z);
        return seamless$wrap == null ? pos : SeamWrap.canonicalChunk(seamless$wrap, pos);
    }
}
