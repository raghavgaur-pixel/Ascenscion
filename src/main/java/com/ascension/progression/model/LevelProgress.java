package com.ascension.progression.model;

/**
 * Immutable view of a character's level progression.
 */
public record LevelProgress(int level, long experience, long experienceIntoLevel, long experienceToNextLevel) {

    public LevelProgress {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (experience < 0L || experienceIntoLevel < 0L || experienceToNextLevel < 0L) {
            throw new IllegalArgumentException("experience values cannot be negative");
        }
    }
}
