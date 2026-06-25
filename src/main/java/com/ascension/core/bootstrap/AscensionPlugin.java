package com.ascension.core.bootstrap;

import org.bukkit.plugin.java.JavaPlugin;

public final class AscensionPlugin extends JavaPlugin {

    private AscensionBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new AscensionBootstrap(this);
        this.bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.disable();
        }
    }
}

