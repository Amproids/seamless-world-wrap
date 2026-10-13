package com.andrewparry.seamlessworldwrap.platform.services;

/** Things common code needs to ask the mod loader. */
public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    java.nio.file.Path getConfigDir();
}
