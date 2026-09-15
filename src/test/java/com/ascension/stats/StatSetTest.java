package com.ascension.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StatSetTest {

    @Test
    void missingStatsDefaultToZero() {
        assertEquals(0.0D, StatSet.empty().get(StatType.STRENGTH));
    }

    @Test
    void plusMergesMatchingAndDistinctStats() {
        final StatSet left = StatSet.copyOf(Map.of(
            StatType.STRENGTH, 10.0D,
            StatType.VITALITY, 5.0D
        ));
        final StatSet right = StatSet.copyOf(Map.of(
            StatType.STRENGTH, 7.5D,
            StatType.INTELLIGENCE, 3.0D
        ));

        final StatSet result = left.plus(right);

        assertEquals(17.5D, result.get(StatType.STRENGTH));
        assertEquals(5.0D, result.get(StatType.VITALITY));
        assertEquals(3.0D, result.get(StatType.INTELLIGENCE));
    }

    @Test
    void nonFiniteValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> StatSet.copyOf(Map.of(StatType.HEALTH, Double.NaN)));
    }
}
