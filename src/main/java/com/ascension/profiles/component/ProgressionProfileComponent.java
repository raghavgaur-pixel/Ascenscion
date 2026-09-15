package com.ascension.profiles.component;

/**
 * Persistent character progression state.
 *
 * <p>Level and experience are character progression only. Tower access is
 * intentionally kept in the separate unlocked-floors component.</p>
 */
public final class ProgressionProfileComponent implements ProfileComponent {

    private int level;
    private long experience;

    public ProgressionProfileComponent() {
        this(1, 0L);
    }

    public ProgressionProfileComponent(final int level, final long experience) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (experience < 0L) {
            throw new IllegalArgumentException("experience cannot be negative");
        }
        this.level = level;
        this.experience = experience;
    }

    public int level() {
        return this.level;
    }

    public long experience() {
        return this.experience;
    }

    public void setLevel(final int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        this.level = level;
    }

    public void setExperience(final long experience) {
        if (experience < 0L) {
            throw new IllegalArgumentException("experience cannot be negative");
        }
        this.experience = experience;
    }

    public long addExperience(final long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("experience gain cannot be negative");
        }
        this.experience = Math.addExact(this.experience, amount);
        return this.experience;
    }

    public boolean levelUp() {
        if (this.level == Integer.MAX_VALUE) {
            return false;
        }
        this.level++;
        return true;
    }
}
