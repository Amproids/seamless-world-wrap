package com.andrewparry.seamlessplanet.mixin.server;

import com.andrewparry.seamlessplanet.wrap.SeamWrap;
import com.andrewparry.seamlessplanet.wrap.SeamWrapHolder;
import net.minecraft.server.level.TickingTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TickingTracker.class)
public abstract class TickingTrackerMixin {

    @ModifyVariable(method = {"addTicket(JLnet/minecraft/server/level/Ticket;)V", "removeTicket(JLnet/minecraft/server/level/Ticket;)V"},
            at = @At("HEAD"), argsOnly = true)
    private long seamless$canonicalTicket(long chunkPos) {
        var wrap = ((SeamWrapHolder) this).seamless$wrap();
        return wrap == null ? chunkPos : SeamWrap.canonicalChunk(wrap, chunkPos);
    }
}
