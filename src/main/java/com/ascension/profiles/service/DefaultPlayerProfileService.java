package com.ascension.profiles.service;

import com.ascension.core.cache.Cache;
import com.ascension.core.logging.PluginLogger;
import com.ascension.profiles.component.ProfileComponentDefinition;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.profiles.repository.PlayerProfileRepository;
import com.ascension.registry.Registry;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default async player profile service with online cache semantics.
 */
public final class DefaultPlayerProfileService implements PlayerProfileService {

    private final PluginLogger logger;
    private final PlayerProfileRepository repository;
    private final Registry<String, ProfileComponentDefinition<?>> componentRegistry;
    private final Cache<UUID, PlayerProfile> onlineProfiles;
    private final ConcurrentHashMap<UUID, CompletableFuture<PlayerProfile>> inflightLoads = new ConcurrentHashMap<>();

    public DefaultPlayerProfileService(
        final PluginLogger logger,
        final PlayerProfileRepository repository,
        final Registry<String, ProfileComponentDefinition<?>> componentRegistry,
        final Cache<UUID, PlayerProfile> onlineProfiles
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.componentRegistry = Objects.requireNonNull(componentRegistry, "componentRegistry");
        this.onlineProfiles = Objects.requireNonNull(onlineProfiles, "onlineProfiles");
    }

    @Override
    public CompletableFuture<PlayerProfile> load(final ProfileLoadRequest request) {
        final Optional<PlayerProfile> cached = this.onlineProfiles.get(request.uniqueId());
        if (cached.isPresent()) {
            final PlayerProfile profile = cached.get();
            profile.updateIdentity(request.username(), request.displayName(), request.joinedAt());
            return CompletableFuture.completedFuture(profile);
        }

        return this.inflightLoads.computeIfAbsent(request.uniqueId(), ignored ->
            this.repository.load(request, this.componentDefinitions())
                .thenApply(profile -> {
                    this.onlineProfiles.put(profile.uniqueId(), profile);
                    return profile;
                })
                .whenComplete((profile, throwable) -> this.inflightLoads.remove(request.uniqueId()))
        );
    }

    @Override
    public CompletableFuture<Void> save(final UUID uniqueId) {
        final Optional<PlayerProfile> profile = this.onlineProfiles.get(uniqueId);
        if (profile.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return this.repository.save(profile.get(), this.componentDefinitions());
    }

    @Override
    public CompletableFuture<Void> unload(final UUID uniqueId) {
        final Optional<PlayerProfile> removed = this.onlineProfiles.remove(uniqueId);
        if (removed.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return this.repository.save(removed.get(), this.componentDefinitions());
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        final CompletableFuture<?>[] futures = this.onlineProfiles.values().stream()
            .map(profile -> this.repository.save(profile, this.componentDefinitions()))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                this.logger.info("Saved " + this.onlineProfiles.values().size() + " cached player profiles.");
            }
        });
    }

    @Override
    public Optional<PlayerProfile> online(final UUID uniqueId) {
        return this.onlineProfiles.get(uniqueId);
    }

    @Override
    public Collection<PlayerProfile> onlineProfiles() {
        return this.onlineProfiles.values();
    }

    private Collection<ProfileComponentDefinition<?>> componentDefinitions() {
        return this.componentRegistry.values();
    }
}
