package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.command.WorldWrapCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class SeamlessWorldWrapFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SeamlessWorldWrap.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> WorldWrapCommands.register(dispatcher));
    }
}
