package com.ascension.progression.service;

/**
 * Calculates the cumulative experience required to reach a character level.
 *
 * <p>The curve is intentionally an injectable contract so server owners can
 * balance progression without changing player persistence or service APIs.</p>
 */
@FunctionalInterface
public interface ExperienceCurve {

    /**
     * Returns cumulative experience required for the supplied level.
     *
     * @param level target level, starting at one
     * @return cumulative experience
     */
    long experienceRequiredForLevel(int level);

    static ExperienceCurve polynomial(
        final long baseExperience,
        final double growth,
        final long flatPerLevel
    ) {
        if (baseExperience < 0L || !Double.isFinite(growth) || growth < 0.0D || flatPerLevel < 0L) {
            throw new IllegalArgumentException("Invalid experience curve parameters");
        }
        return level -> {
            if (level < 1) {
                throw new IllegalArgumentException("level must be at least 1");
            }
            if (level == 1) {
                return 0L;
            }
            final double exponent = Math.pow(level - 1D, 1D + growth);
            final double result = baseExperience * exponent + flatPerLevel * (double) (level - 1);
            if (!Double.isFinite(result) || result >= Long.MAX_VALUE) {
                throw new ArithmeticException("Experience curve overflow at level " + level);
            }
            return Math.max(0L, Math.round(result));
        };
    }
}
