package com.ascension.tower.service;

import com.ascension.assets.definition.FloorDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.registry.Registry;
import com.ascension.tower.model.FloorId;
import java.util.Objects;

/**
 * Persistent tower-access service.
 *
 * <p>Floor definitions own content metadata; this service owns the rule that
 * determines whether a player may enter a floor. Completion state can later be
 * expanded without changing floor assets.</p>
 */
public final class TowerService {

    private static final String PREREQUISITE_KEY = "prerequisite_floor";

    private final Registry<AssetId, FloorDefinition> floors;

    public TowerService(final Registry<AssetId, FloorDefinition> floors) {
        this.floors = Objects.requireNonNull(floors, "floors");
    }

    /**
     * Checks whether a floor exists and the player has unlocked it.
     */
    public boolean canEnter(
        final UnlockedFloorsProfileComponent progression,
        final FloorId floorId
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(floorId, "floorId");
        return this.floors.find(AssetId.parse(floorId.value())).isPresent()
            && progression.snapshot().contains(floorId.value());
    }

    /**
     * Attempts to unlock a floor using the prerequisite declared by its content
     * definition. A floor without a prerequisite is an initial entry point.
     */
    public UnlockResult unlock(
        final UnlockedFloorsProfileComponent progression,
        final FloorId floorId
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(floorId, "floorId");

        final FloorDefinition definition;
        try {
            definition = this.floors.require(AssetId.parse(floorId.value()));
        } catch (final IllegalArgumentException | IllegalStateException exception) {
            return UnlockResult.rejected("Unknown floor: " + floorId);
        }

        if (progression.snapshot().contains(floorId.value())) {
            return UnlockResult.alreadyUnlocked();
        }

        final String prerequisite = definition.data().getString(PREREQUISITE_KEY, "");
        if (!prerequisite.isBlank() && !progression.snapshot().contains(prerequisite)) {
            return UnlockResult.rejected("Prerequisite floor is not unlocked: " + prerequisite);
        }

        final boolean added = progression.unlock(floorId.value());
        return added ? UnlockResult.success() : UnlockResult.alreadyUnlocked();
    }

    public record UnlockResult(Status status, String reason) {

        public UnlockResult {
            Objects.requireNonNull(status, "status");
            reason = reason == null ? "" : reason;
        }

        public static UnlockResult success() {
            return new UnlockResult(Status.UNLOCKED, "");
        }

        public static UnlockResult alreadyUnlocked() {
            return new UnlockResult(Status.ALREADY_UNLOCKED, "");
        }

        public static UnlockResult rejected(final String reason) {
            return new UnlockResult(Status.REJECTED, reason);
        }

        public enum Status {
            UNLOCKED,
            ALREADY_UNLOCKED,
            REJECTED
        }
    }
}
