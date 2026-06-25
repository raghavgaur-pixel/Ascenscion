package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Typed contract for modular profile components that can be registered by subsystems.
 *
 * @param <T> component type
 */
public interface ProfileComponentDefinition<T extends ProfileComponent> {

    /**
     * @return stable component identifier
     */
    String id();

    /**
     * @return runtime component class
     */
    Class<T> type();

    /**
     * Creates the default value for a newly created profile.
     *
     * @param request profile load request
     * @return default component
     */
    T createDefault(ProfileLoadRequest request);

    /**
     * Serializes a component instance.
     *
     * @param component component instance
     * @return serialized data
     */
    SerializedObject serialize(T component);

    /**
     * Deserializes a component instance.
     *
     * @param data serialized data
     * @return deserialized component
     */
    T deserialize(SerializedObject data);
}

