package com.andrewparry.seamlessplanet;

import com.andrewparry.seamlessplanet.command.SeamlessCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class SeamlessPlanetFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SeamlessPlanet.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SeamlessCommands.register(dispatcher));
    }
}
