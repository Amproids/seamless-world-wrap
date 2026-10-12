package com.andrewparry.seamlessplanet;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(SeamlessPlanet.MOD_ID)
public class SeamlessPlanetNeoForge {

    public SeamlessPlanetNeoForge(IEventBus eventBus) {
        SeamlessPlanet.init();
    }
}
