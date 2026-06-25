package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Definition for achievement progress.
 */
public final class AchievementsProfileComponentDefinition
    implements ProfileComponentDefinition<AchievementsProfileComponent> {

    @Override
    public String id() {
        return "achievements";
    }

    @Override
    public Class<AchievementsProfileComponent> type() {
        return AchievementsProfileComponent.class;
    }

    @Override
    public AchievementsProfileComponent createDefault(final ProfileLoadRequest request) {
        return new AchievementsProfileComponent();
    }

    @Override
    public SerializedObject serialize(final AchievementsProfileComponent component) {
        return SerializedObject.builder()
            .put("achievement_ids", component.snapshot())
            .build();
    }

    @Override
    public AchievementsProfileComponent deserialize(final SerializedObject data) {
        return new AchievementsProfileComponent(data.getStringSet("achievement_ids"));
    }
}

