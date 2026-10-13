package com.andrewparry.seamlessworldwrap.mixin.server;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk generation works on a grid of neighbor holders around the chunk being generated. Near a
 * seam, code asks for a neighbor by its canonical position, which isn't on the grid, though the
 * same holder is (at its alias). Vanilla would throw for any out-of-range request, so only those
 * requests are answered, by finding the holder whose position is the one asked for.
 */
@Mixin(StaticCache2D.class)
public abstract class StaticCache2DMixin<T> {

    @Shadow
    @Final
    private Object[] cache;

    @Shadow
    public abstract boolean contains(int x, int z);

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void seamless$findAlias(int x, int z, CallbackInfoReturnable<T> cir) {
        if (contains(x, z)) {
            return;
        }
        for (Object value : cache) {
            if (value instanceof GenerationChunkHolder holder && holder.getPos().x == x && holder.getPos().z == z) {
                cir.setReturnValue((T) value);
                return;
            }
        }
    }
}
