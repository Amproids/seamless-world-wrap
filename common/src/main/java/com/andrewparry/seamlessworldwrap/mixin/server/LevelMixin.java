package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Writes and block entity lookups use canonical positions, so block entities, block updates and
 * scheduled ticks are keyed consistently no matter which side of the seam they came from.
 */
@Mixin(Level.class)
public abstract class LevelMixin {

    @ModifyVariable(method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            "getBlockEntity", "removeBlockEntity"},
            at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$canonicalPos(BlockPos pos) {
        WrapSettings wrap = SeamWrap.of((Level) (Object) this);
        return wrap == null ? pos : SeamWrap.canonical(wrap, pos);
    }
}
