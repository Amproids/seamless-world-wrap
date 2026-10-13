package com.andrewparry.seamlessworldwrap.mixin.client;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Some blocks vary by position: which texture rotation or model variant they use, and how far
 * flowers and grass are nudged off-center. Base both on the canonical position, so a block looks
 * the same at every copy of the world and nothing flickers when a section is rebuilt one world
 * size over after a seam crossing.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateVariationMixin {

    @ModifyVariable(method = "getSeed", at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$canonicalSeed(BlockPos pos) {
        WrapSettings wrap = ClientWrapState.current();
        return wrap == null ? pos : SeamWrap.canonical(wrap, pos);
    }

    @ModifyVariable(method = "getOffset", at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$canonicalOffset(BlockPos pos, BlockGetter level) {
        WrapSettings wrap = ClientWrapState.current();
        return wrap == null ? pos : SeamWrap.canonical(wrap, pos);
    }
}
