package com.ascension.core.platform;

import java.io.File;

public interface PluginPlatform {

    File dataFolder();

    void ensureDataDirectories();

    boolean isPluginEnabled(String pluginName);
}

