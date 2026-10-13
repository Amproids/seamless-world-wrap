package com.andrewparry.seamlessworldwrap.mixin.packet;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMoveVehiclePacket.class)
public interface ServerboundMoveVehiclePacketAccessor {

    @Mutable
    @Accessor("x")
    void seamless$setX(double x);

    @Mutable
    @Accessor("z")
    void seamless$setZ(double z);
}
