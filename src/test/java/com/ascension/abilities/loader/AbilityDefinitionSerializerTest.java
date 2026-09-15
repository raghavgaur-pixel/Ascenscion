package com.ascension.abilities.loader;

import com.ascension.abilities.model.AbilityTargetType;
import com.ascension.assets.loader.AssetSource;
import com.ascension.assets.model.AssetId;
import com.ascension.serialization.SerializedFormat;
import com.ascension.serialization.SerializedObject;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityDefinitionSerializerTest {

    @Test
    void deserializesDataDrivenAbilityDocument() {
        final SerializedObject object = SerializedObject.builder()
            .put("id", "ascension:test_strike")
            .put("version", "1.0.0")
            .put("display_name", "Test Strike")
            .put("description", "Test ability")
            .put("target", "single_entity")
            .put("cost", Map.of("mana", 12.5D))
            .put("cooldown_millis", 2500L)
            .put("data", Map.of("behavior", "physical_attack"))
            .build();

        final var definition = new AbilityDefinitionSerializer().deserialize(
            new AssetSource(Path.of("test.yml"), SerializedFormat.YAML),
            object,
            "test"
        );

        assertEquals(new AssetId("ascension", "test_strike"), definition.id());
        assertEquals(AbilityTargetType.SINGLE_ENTITY, definition.targetType());
        assertEquals(12.5D, definition.cost().resources().get("mana"));
        assertEquals(2500L, definition.cooldownMillis());
        assertEquals("physical_attack", definition.data().getString("behavior", ""));
    }
}
