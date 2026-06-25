package com.ascension.profiles.repository;

import com.ascension.profiles.component.ProfileComponentDefinition;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.model.ProfileLoadRequest;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Aggregate repository for player profiles and modular component payloads.
 */
public interface PlayerProfileRepository {

    /**
     * Loads or creates a profile aggregate.
     *
     * @param request profile load request
     * @param componentDefinitions registered component definitions
     * @return loaded profile
     */
    CompletableFuture<PlayerProfile> load(ProfileLoadRequest request, Collection<ProfileComponentDefinition<?>> componentDefinitions);

    /**
     * Saves a profile aggregate.
     *
     * @param profile profile aggregate
     * @param componentDefinitions registered component definitions
     * @return completion future
     */
    CompletableFuture<Void> save(PlayerProfile profile, Collection<ProfileComponentDefinition<?>> componentDefinitions);
}

