package com.ascension.tower.service;

import com.ascension.assets.definition.FloorDefinition;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.profiles.component.UnlockedFloorsProfileComponent;
import com.ascension.registry.ConcurrentMutableRegistry;
import com.ascension.registry.RegistryDescriptor;
import com.ascension.serialization.SerializedObject;
import com.ascension.tower.model.FloorId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowerServiceTest {

    private static FloorDefinition floor(final String id, final String prerequisite) {
        return new FloorDefinition(
            new AssetDescriptor(
                AssetId.parse(id),
                "core",
                new SemanticVersion(1, 0, 0),
                id,
                "test floor",
                java.util.Map.of(),
                AssetCompatibility.open(),
                java.util.Set.of()
            ),
            prerequisite.isBlank()
                ? SerializedObject.empty()
                : SerializedObject.builder().put("prerequisite_floor", prerequisite).build()
        );
    }

    @Test
    void unlockHonorsPrerequisiteAndAllowsInitialFloor() {
        final var registry = new ConcurrentMutableRegistry<AssetId, FloorDefinition>(
            new RegistryDescriptor<>("floors", AssetId.class, FloorDefinition.class)
        );
        registry.register(AssetId.parse("ascension:floor_1"), floor("ascension:floor_1", ""));
        registry.register(AssetId.parse("ascension:floor_2"), floor("ascension:floor_2", "ascension:floor_1"));

        final var service = new TowerService(registry);
        final var unlocked = new UnlockedFloorsProfileComponent();

        assertEquals(
            TowerService.UnlockResult.Status.UNLOCKED,
            service.unlock(unlocked, FloorId.of("ascension:floor_1")).status()
        );
        assertTrue(service.canEnter(unlocked, FloorId.of("ascension:floor_1")));

        assertEquals(
            TowerService.UnlockResult.Status.UNLOCKED,
            service.unlock(unlocked, FloorId.of("ascension:floor_2")).status()
        );
        assertTrue(service.canEnter(unlocked, FloorId.of("ascension:floor_2")));
    }

    @Test
    void lockedFloorCannotBeEnteredAndPrerequisiteBlocksUnlock() {
        final var registry = new ConcurrentMutableRegistry<AssetId, FloorDefinition>(
            new RegistryDescriptor<>("floors", AssetId.class, FloorDefinition.class)
        );
        registry.register(AssetId.parse("ascension:floor_1"), floor("ascension:floor_1", ""));
        registry.register(AssetId.parse("ascension:floor_2"), floor("ascension:floor_2", "ascension:floor_1"));

        final var service = new TowerService(registry);
        final var unlocked = new UnlockedFloorsProfileComponent();

        assertEquals(
            TowerService.UnlockResult.Status.REJECTED,
            service.unlock(unlocked, FloorId.of("ascension:floor_2")).status()
        );
        assertTrue(!service.canEnter(unlocked, FloorId.of("ascension:floor_2")));
    }
}
