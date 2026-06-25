package com.ascension.profiles.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-configurable settings state.
 */
public final class SettingsProfileComponent implements ProfileComponent {

    private final Map<String, String> settings;

    public SettingsProfileComponent() {
        this.settings = new ConcurrentHashMap<>();
    }

    public SettingsProfileComponent(final Map<String, String> settings) {
        this.settings = new ConcurrentHashMap<>(settings);
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(this.settings);
    }

    public void put(final String key, final String value) {
        this.settings.put(key, value);
    }
}

