package com.ascension.registry;

import com.ascension.database.migration.SchemaMigration;
import com.ascension.profiles.component.ProfileComponentDefinition;

/**
 * Canonical registry descriptors used by the foundation modules.
 */
public final class AscensionRegistries {

    public static final RegistryDescriptor<String, SchemaMigration> SCHEMA_MIGRATIONS =
        new RegistryDescriptor<>("schema_migrations", String.class, SchemaMigration.class);

    public static final RegistryDescriptor<String, ProfileComponentDefinition<?>> PROFILE_COMPONENTS =
        new RegistryDescriptor<>("profile_components", String.class, castProfileComponentDefinitionType());

    private AscensionRegistries() {
    }

    @SuppressWarnings("unchecked")
    private static Class<ProfileComponentDefinition<?>> castProfileComponentDefinitionType() {
        return (Class<ProfileComponentDefinition<?>>) (Class<?>) ProfileComponentDefinition.class;
    }
}

