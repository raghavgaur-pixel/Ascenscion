package com.ascension.profiles.component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks floor progression unlocks.
 */
public final class UnlockedFloorsProfileComponent implements ProfileComponent {

    private final Set<String> unlockedFloorIds;

    public UnlockedFloorsProfileComponent() {
        this.unlockedFloorIds = ConcurrentHashMap.newKeySet();
    }

    public UnlockedFloorsProfileComponent(final Set<String> unlockedFloorIds) {
        this.unlockedFloorIds = ConcurrentHashMap.newKeySet();
        this.unlockedFloorIds.addAll(unlockedFloorIds);
    }

    public boolean unlock(final String floorId) {
        return this.unlockedFloorIds.add(floorId);
    }

    public Set<String> snapshot() {
        return Set.copyOf(this.unlockedFloorIds);
    }
}

