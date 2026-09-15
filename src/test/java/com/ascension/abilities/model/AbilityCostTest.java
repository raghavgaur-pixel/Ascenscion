package com.ascension.abilities.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AbilityCostTest {

    @Test
    void noneHasNoResources() {
        assertEquals(Map.of(), AbilityCost.none().resources());
    }

    @Test
    void negativeCostsAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new AbilityCost(Map.of("mana", -1.0D)));
    }

    @Test
    void nonFiniteCostsAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new AbilityCost(Map.of("mana", Double.POSITIVE_INFINITY)));
    }
}
