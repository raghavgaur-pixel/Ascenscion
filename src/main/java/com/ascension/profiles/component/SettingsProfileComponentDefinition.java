package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Definition for settings profile state.
 */
public final class SettingsProfileComponentDefinition implements ProfileComponentDefinition<SettingsProfileComponent> {

    @Override
    public String id() {
        return "settings";
    }

    @Override
    public Class<SettingsProfileComponent> type() {
        return SettingsProfileComponent.class;
    }

    @Override
    public SettingsProfileComponent createDefault(final ProfileLoadRequest request) {
        return new SettingsProfileComponent();
    }

    @Override
    public SerializedObject serialize(final SettingsProfileComponent component) {
        return SerializedObject.builder()
            .put("settings", component.snapshot())
            .build();
    }

    @Override
    public SettingsProfileComponent deserialize(final SerializedObject data) {
        return new SettingsProfileComponent(data.getStringMap("settings"));
    }
}

