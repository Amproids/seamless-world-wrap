package com.andrewparry.seamlessplanet;

import net.fabricmc.api.ModInitializer;

public class SeamlessPlanetFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SeamlessPlanet.init();
    }
}
