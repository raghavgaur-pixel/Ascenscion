package com.ascension.profiles.component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks unlocked achievements.
 */
public final class AchievementsProfileComponent implements ProfileComponent {

    private final Set<String> achievementIds;

    public AchievementsProfileComponent() {
        this.achievementIds = ConcurrentHashMap.newKeySet();
    }

    public AchievementsProfileComponent(final Set<String> achievementIds) {
        this.achievementIds = ConcurrentHashMap.newKeySet();
        this.achievementIds.addAll(achievementIds);
    }

    public boolean add(final String achievementId) {
        return this.achievementIds.add(achievementId);
    }

    public Set<String> snapshot() {
        return Set.copyOf(this.achievementIds);
    }
}

