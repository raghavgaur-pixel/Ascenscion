package com.ascension.progression.service;

import com.ascension.profiles.component.ProgressionProfileComponent;
import com.ascension.progression.model.LevelProgress;
import java.util.Objects;

/**
 * Applies character experience and level progression without coupling to a
 * particular UI, content pack, or world implementation.
 */
public final class ProgressionService {

    private final ExperienceCurve experienceCurve;
    private final int maximumLevel;

    public ProgressionService(final ExperienceCurve experienceCurve, final int maximumLevel) {
        this.experienceCurve = Objects.requireNonNull(experienceCurve, "experienceCurve");
        if (maximumLevel < 1) {
            throw new IllegalArgumentException("maximumLevel must be at least 1");
        }
        this.maximumLevel = maximumLevel;
    }

    /**
     * Grants experience and applies all resulting level-ups.
     *
     * @return number of levels gained
     */
    public int grantExperience(final ProgressionProfileComponent progression, final long amount) {
        Objects.requireNonNull(progression, "progression");
        if (amount < 0L) {
            throw new IllegalArgumentException("experience gain cannot be negative");
        }
        progression.addExperience(amount);

        int levelsGained = 0;
        while (progression.level() < this.maximumLevel
            && progression.experience() >= this.experienceCurve.experienceRequiredForLevel(progression.level() + 1)) {
            if (!progression.levelUp()) {
                break;
            }
            levelsGained++;
        }
        return levelsGained;
    }

    public LevelProgress progress(final ProgressionProfileComponent progression) {
        Objects.requireNonNull(progression, "progression");
        final int level = progression.level();
        final long currentThreshold = this.experienceCurve.experienceRequiredForLevel(level);
        final long nextThreshold = level >= this.maximumLevel
            ? currentThreshold
            : this.experienceCurve.experienceRequiredForLevel(level + 1);
        final long intoLevel = Math.max(0L, progression.experience() - currentThreshold);
        final long toNext = Math.max(0L, nextThreshold - currentThreshold);
        return new LevelProgress(level, progression.experience(), intoLevel, toNext);
    }
}
