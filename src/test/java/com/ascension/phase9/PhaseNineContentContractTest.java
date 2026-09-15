package com.ascension.phase9;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PhaseNineContentContractTest {

    private static final List<String> REQUIRED_ASSETS = List.of(
        "assets/floors/floor_001.yml",
        "assets/floors/floor_002.yml",
        "assets/items/rookie_sword.yml",
        "assets/items/rookie_iron_ring.yml",
        "assets/abilities/quick_strike.yml",
        "assets/abilities/power_slash.yml",
        "assets/abilities/gatebreaker.yml",
        "assets/abilities/iron_fan.yml",
        "assets/abilities/warden_roar.yml",
        "assets/abilities/last_stand.yml",
        "assets/mobs/forest_wolf.yml",
        "assets/mobs/iron_beetle.yml",
        "assets/bosses/warden_of_the_first_gate.yml",
        "assets/effects/shaken.yml",
        "assets/effects/warden_enraged.yml",
        "assets/quests/arrival.yml",
        "assets/quests/first_hunt.yml",
        "assets/quests/the_first_gate.yml",
        "assets/loot_tables/forest_wolf.yml",
        "assets/loot_tables/iron_beetle.yml"
    );

    @Test
    void allPhaseNineAssetsArePackaged() {
        for (final String resource : REQUIRED_ASSETS) {
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, "Missing required Phase 9 asset: " + resource);
            } catch (Exception exception) {
                throw new AssertionError("Could not inspect resource: " + resource, exception);
            }
        }
    }
}
