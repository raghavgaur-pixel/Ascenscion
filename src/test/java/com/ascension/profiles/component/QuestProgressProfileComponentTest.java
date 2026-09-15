package com.ascension.profiles.component;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProgressProfileComponentTest {

    @Test
    void tracksActiveCompletedAndObjectiveProgress() {
        final QuestProgressProfileComponent state = new QuestProgressProfileComponent();
        state.accept("ascension:arrival");
        assertTrue(state.isActive("ascension:arrival"));
        assertEquals(2, state.addProgress("ascension:first_hunt:0", 2));
        state.complete("ascension:arrival");
        assertTrue(state.isCompleted("ascension:arrival"));
        assertEquals(Set.of("ascension:arrival"), state.completedSnapshot());
        assertEquals(Map.of("ascension:first_hunt:0", 2), state.progressSnapshot());
    }

    @Test
    void definitionRoundTripsPersistentState() {
        final QuestProgressProfileComponent original = new QuestProgressProfileComponent(
            Map.of("ascension:first_hunt:0", 5),
            Set.of("ascension:arrival"),
            Set.of("ascension:first_hunt")
        );
        final QuestProgressProfileComponent.Definition definition = new QuestProgressProfileComponent.Definition();
        final QuestProgressProfileComponent restored = definition.deserialize(definition.serialize(original));
        assertEquals(original.progressSnapshot(), restored.progressSnapshot());
        assertEquals(original.completedSnapshot(), restored.completedSnapshot());
        assertEquals(original.activeSnapshot(), restored.activeSnapshot());
    }
}
