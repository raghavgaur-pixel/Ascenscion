package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Definition for generic numeric statistics.
 */
public final class StatisticsProfileComponentDefinition
    implements ProfileComponentDefinition<StatisticsProfileComponent> {

    @Override
    public String id() {
        return "statistics";
    }

    @Override
    public Class<StatisticsProfileComponent> type() {
        return StatisticsProfileComponent.class;
    }

    @Override
    public StatisticsProfileComponent createDefault(final ProfileLoadRequest request) {
        return new StatisticsProfileComponent();
    }

    @Override
    public SerializedObject serialize(final StatisticsProfileComponent component) {
        return SerializedObject.builder()
            .put("counters", component.snapshot())
            .build();
    }

    @Override
    public StatisticsProfileComponent deserialize(final SerializedObject data) {
        return new StatisticsProfileComponent(data.getLongMap("counters"));
    }
}

