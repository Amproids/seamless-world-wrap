package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.wrap.ChunkLocal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** See ChunkLocal. */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessBlockEntityMixin {

    @Inject(method = "setBlockEntityNbt(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
    private void seamless$ownNbt(CompoundTag tag, CallbackInfo ci) {
        ChunkLocal.ownTag(((ChunkAccess) (Object) this).getPos(), tag);
    }

    @ModifyVariable(method = "getBlockEntityNbt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;", at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$ownPos(BlockPos pos) {
        return ChunkLocal.own(((ChunkAccess) (Object) this).getPos(), pos);
    }
}
