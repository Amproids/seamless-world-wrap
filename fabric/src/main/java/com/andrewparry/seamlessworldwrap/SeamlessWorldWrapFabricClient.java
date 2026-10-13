package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.network.ClientWrapState;
import com.andrewparry.seamlessworldwrap.network.WrapSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SeamlessWorldWrapFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(WrapSyncPayload.TYPE, (payload, context) -> ClientWrapState.apply(payload));
    }
}
