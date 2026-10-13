package com.andrewparry.seamlessworldwrap;

import com.andrewparry.seamlessworldwrap.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared entry point; each loader's bootstrap class calls {@link #init()}. */
public class SeamlessWorldWrap {

    public static final String MOD_ID = "seamless_world_wrap";
    public static final String MOD_NAME = "Seamless World Wrap";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    // Until there's a world-creation screen, every world uses config/seamless_world_wrap.properties.
    // Changing it for an existing world moves its seams, so it's for experimenting only.
    private static WrapSettings settings = WrapSettings.DEFAULT;

    public static WrapSettings settings() {
        return settings;
    }

    public static void init() {
        settings = WrapConfig.load(Services.PLATFORM.getConfigDir().resolve(MOD_ID + ".properties"));
        LOG.info("{} loaded on {}: {}", MOD_NAME, Services.PLATFORM.getPlatformName(), settings);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
