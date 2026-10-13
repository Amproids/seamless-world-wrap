package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.command.WorldWrapCommands;
import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import com.andrewparry.seamlessworldwrap.network.WrapSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(SeamlessWorldWrap.MOD_ID)
public class SeamlessWorldWrapNeoForge {

    public SeamlessWorldWrapNeoForge(IEventBus eventBus) {
        SeamlessWorldWrap.init();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WorldWrapCommands.register(event.getDispatcher()));

        // Optional, so players without the mod can still join a wrapped server.
        eventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional()
                .playToClient(WrapSyncPayload.TYPE, WrapSyncPayload.CODEC, (payload, context) -> ClientWrapState.apply(payload)));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player && player.connection.hasChannel(WrapSyncPayload.TYPE)) {
                PacketDistributor.sendToPlayer(player, WrapSyncPayload.current());
            }
        });
    }
}
