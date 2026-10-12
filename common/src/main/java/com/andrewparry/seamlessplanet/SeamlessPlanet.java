package com.andrewparry.seamlessplanet;

import com.andrewparry.seamlessplanet.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point; each loader's bootstrap class calls {@link #init()}. */
public class SeamlessPlanet {

    public static final String MOD_ID = "seamless_planet";
    public static final String MOD_NAME = "Seamless Planet";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOG.info("{} loaded on {}", MOD_NAME, Services.PLATFORM.getPlatformName());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
