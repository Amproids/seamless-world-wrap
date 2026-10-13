package com.andrewparry.seamlessplanet.mixin.server;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface ServerEntityAccessor {

    @Accessor("positionCodec")
    VecDeltaCodec seamless$positionCodec();
}
