package com.ascension.profiles.service;

import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.model.ProfileLoadRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Runtime service for loading, caching, and saving player profiles.
 */
public interface PlayerProfileService {

    /**
     * Loads or creates a player profile and caches it for online access.
     *
     * @param request load request
     * @return loaded profile
     */
    CompletableFuture<PlayerProfile> load(ProfileLoadRequest request);

    /**
     * Saves a cached profile.
     *
     * @param uniqueId player uuid
     * @return completion future
     */
    CompletableFuture<Void> save(UUID uniqueId);

    /**
     * Saves and removes a cached profile.
     *
     * @param uniqueId player uuid
     * @return completion future
     */
    CompletableFuture<Void> unload(UUID uniqueId);

    /**
     * Saves every cached profile.
     *
     * @return completion future
     */
    CompletableFuture<Void> saveAll();

    /**
     * Reads a cached profile if it is currently online.
     *
     * @param uniqueId player uuid
     * @return cached profile if present
     */
    Optional<PlayerProfile> online(UUID uniqueId);

    /**
     * @return snapshot of cached online profiles
     */
    Collection<PlayerProfile> onlineProfiles();
}

