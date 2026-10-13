package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.command.WorldWrapCommands;
import com.andrewparry.seamlessworldwrap.network.WrapSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class SeamlessWorldWrapFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SeamlessWorldWrap.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WorldWrapCommands.register(dispatcher));

        PayloadTypeRegistry.playS2C().register(WrapSyncPayload.TYPE, WrapSyncPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler, WrapSyncPayload.TYPE)) {
                sender.sendPacket(WrapSyncPayload.current());
            }
        });
    }
}
