package com.ascension.profiles.component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable component registry attached to an individual player profile.
 */
public final class ProfileComponentContainer {

    private final Map<String, ProfileComponent> components;

    public ProfileComponentContainer(final Map<String, ProfileComponent> components) {
        this.components = Map.copyOf(components);
    }

    public <T extends ProfileComponent> T require(final ProfileComponentDefinition<T> definition) {
        Objects.requireNonNull(definition, "definition");
        final ProfileComponent component = this.components.get(definition.id());
        if (component == null) {
            throw new IllegalStateException("Missing profile component: " + definition.id());
        }
        return definition.type().cast(component);
    }

    public Optional<ProfileComponent> find(final String componentId) {
        return Optional.ofNullable(this.components.get(componentId));
    }

    public Collection<ProfileComponent> values() {
        return this.components.values();
    }

    public Map<String, ProfileComponent> asMap() {
        return this.components;
    }
}

