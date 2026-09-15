package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Persistence definition for character level and experience.
 */
public final class ProgressionProfileComponentDefinition
    implements ProfileComponentDefinition<ProgressionProfileComponent> {

    @Override
    public String id() {
        return "progression";
    }

    @Override
    public Class<ProgressionProfileComponent> type() {
        return ProgressionProfileComponent.class;
    }

    @Override
    public ProgressionProfileComponent createDefault(final ProfileLoadRequest request) {
        return new ProgressionProfileComponent();
    }

    @Override
    public SerializedObject serialize(final ProgressionProfileComponent component) {
        return SerializedObject.builder()
            .put("level", component.level())
            .put("experience", component.experience())
            .build();
    }

    @Override
    public ProgressionProfileComponent deserialize(final SerializedObject data) {
        return new ProgressionProfileComponent(
            Math.toIntExact(data.getLong("level", 1L)),
            data.getLong("experience", 0L)
        );
    }
}
