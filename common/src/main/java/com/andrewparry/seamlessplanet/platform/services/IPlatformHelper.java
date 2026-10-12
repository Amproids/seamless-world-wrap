package com.andrewparry.seamlessplanet.platform.services;

/** Things common code needs to ask the mod loader. */
public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();
}
