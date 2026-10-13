package com.andrewparry.seamlessworldwrap.mixin.server;

import com.andrewparry.seamlessworldwrap.wrap.SeamPacketTranslator;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every clientbound game packet passes through here; positions are moved into the player's frame. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"), argsOnly = true)
    private Packet<?> seamless$toClientFrame(Packet<?> packet) {
        if ((Object) this instanceof ServerGamePacketListenerImpl game && game.player != null) {
            return SeamPacketTranslator.toClient(packet, game.player);
        }
        return packet;
    }
}
