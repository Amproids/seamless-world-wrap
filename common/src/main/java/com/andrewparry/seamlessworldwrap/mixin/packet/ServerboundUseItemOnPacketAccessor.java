package com.andrewparry.seamlessworldwrap.mixin.packet;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemOnPacket.class)
public interface ServerboundUseItemOnPacketAccessor {

    @Mutable
    @Accessor("blockHit")
    void seamless$setBlockHit(BlockHitResult hit);
}
