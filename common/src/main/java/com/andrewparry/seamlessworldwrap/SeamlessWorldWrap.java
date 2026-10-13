package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** Shared entry point; each loader's bootstrap class calls {@link #init()}. */
public class SeamlessWorldWrap {

    public static final String MOD_ID = "seamless_world_wrap";
    public static final String MOD_NAME = "Seamless World Wrap";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOG.info("{} loaded on {}", MOD_NAME, Services.PLATFORM.getPlatformName());
    }

    public static Path configFile() {
        return Services.PLATFORM.getConfigDir().resolve(MOD_ID + ".properties");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
