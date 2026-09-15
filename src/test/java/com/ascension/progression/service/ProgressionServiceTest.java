package com.ascension.progression.service;

import com.ascension.profiles.component.ProgressionProfileComponent;
import com.ascension.progression.model.LevelProgress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionServiceTest {

    @Test
    void grantsExperienceAndAppliesMultipleLevels() {
        final ExperienceCurve curve = level -> switch (level) {
            case 1 -> 0L;
            case 2 -> 100L;
            case 3 -> 250L;
            case 4 -> 450L;
            default -> throw new IllegalArgumentException("unexpected test level");
        };
        final ProgressionService service = new ProgressionService(curve, 10);
        final ProgressionProfileComponent progression = new ProgressionProfileComponent();

        assertEquals(2, service.grantExperience(progression, 300L));
        assertEquals(3, progression.level());
        assertEquals(300L, progression.experience());

        final LevelProgress view = service.progress(progression);
        assertEquals(3, view.level());
        assertEquals(50L, view.experienceIntoLevel());
        assertEquals(200L, view.experienceToNextLevel());
    }

    @Test
    void maxLevelStopsFurtherLevelUps() {
        final ExperienceCurve curve = level -> (level - 1L) * 100L;
        final ProgressionService service = new ProgressionService(curve, 3);
        final ProgressionProfileComponent progression = new ProgressionProfileComponent(3, 100_000L);

        assertEquals(0, service.grantExperience(progression, 1L));
        assertEquals(3, progression.level());
        assertEquals(100_001L, progression.experience());
        assertEquals(0L, service.progress(progression).experienceToNextLevel());
    }
}
