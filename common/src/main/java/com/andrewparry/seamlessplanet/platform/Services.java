package com.andrewparry.seamlessplanet.platform;

import com.andrewparry.seamlessplanet.SeamlessPlanet;
import com.andrewparry.seamlessplanet.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Loads loader-specific implementations of common interfaces. Each loader lists its
 * implementation in META-INF/services/&lt;interface name&gt;.
 */
public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        SeamlessPlanet.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
