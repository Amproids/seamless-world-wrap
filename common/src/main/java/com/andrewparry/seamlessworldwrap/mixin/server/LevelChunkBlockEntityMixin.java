package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.wrap.ChunkLocal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** See ChunkLocal. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkBlockEntityMixin {

    @ModifyVariable(method = {"getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;", "removeBlockEntity(Lnet/minecraft/core/BlockPos;)V", "getBlockEntityNbtForSaving(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;"}, at = @At("HEAD"), argsOnly = true)
    private BlockPos seamless$ownPos(BlockPos pos) {
        return ChunkLocal.own(((ChunkAccess) (Object) this).getPos(), pos);
    }

    @Inject(method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"))
    private void seamless$ownEntity(BlockEntity blockEntity, CallbackInfo ci) {
        ChunkLocal.ownEntity(((ChunkAccess) (Object) this).getPos(), blockEntity);
    }
}
