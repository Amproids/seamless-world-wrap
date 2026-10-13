package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.command.WorldWrapCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(SeamlessWorldWrap.MOD_ID)
public class SeamlessWorldWrapNeoForge {

    public SeamlessWorldWrapNeoForge(IEventBus eventBus) {
        SeamlessWorldWrap.init();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WorldWrapCommands.register(event.getDispatcher()));
    }
}
