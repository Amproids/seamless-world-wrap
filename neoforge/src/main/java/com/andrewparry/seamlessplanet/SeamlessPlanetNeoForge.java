package com.andrewparry.seamlessplanet;

import com.andrewparry.seamlessplanet.command.SeamlessCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SeamlessPlanet.MOD_ID)
public class SeamlessPlanetNeoForge {

    public SeamlessPlanetNeoForge(IEventBus eventBus) {
        SeamlessPlanet.init();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> SeamlessCommands.register(event.getDispatcher()));
    }
}
