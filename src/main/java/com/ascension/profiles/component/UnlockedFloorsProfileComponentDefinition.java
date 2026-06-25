package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Definition for floor progression profile state.
 */
public final class UnlockedFloorsProfileComponentDefinition
    implements ProfileComponentDefinition<UnlockedFloorsProfileComponent> {

    @Override
    public String id() {
        return "unlocked_floors";
    }

    @Override
    public Class<UnlockedFloorsProfileComponent> type() {
        return UnlockedFloorsProfileComponent.class;
    }

    @Override
    public UnlockedFloorsProfileComponent createDefault(final ProfileLoadRequest request) {
        return new UnlockedFloorsProfileComponent();
    }

    @Override
    public SerializedObject serialize(final UnlockedFloorsProfileComponent component) {
        return SerializedObject.builder()
            .put("floor_ids", component.snapshot())
            .build();
    }

    @Override
    public UnlockedFloorsProfileComponent deserialize(final SerializedObject data) {
        return new UnlockedFloorsProfileComponent(data.getStringSet("floor_ids"));
    }
}

