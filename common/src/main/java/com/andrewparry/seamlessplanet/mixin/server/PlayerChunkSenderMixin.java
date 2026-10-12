package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.WrapSettings;
import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Pending chunks are keyed by canonical position; views drop them by alias. */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @ModifyVariable(method = "dropChunk", at = @At("HEAD"), argsOnly = true)
    private ChunkPos seamless$canonicalDrop(ChunkPos pos, ServerPlayer player) {
        WrapSettings wrap = SeamWrap.of(player.level());
        return wrap == null ? pos : SeamWrap.canonicalChunk(wrap, pos);
    }
}
