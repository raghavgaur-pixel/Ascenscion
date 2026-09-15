package com.ascension.phase9;

import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.events.EventBus;
import com.ascension.events.EventPriority;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.service.ItemService;
import com.ascension.profiles.service.PlayerProfileService;
import com.ascension.progression.service.ExperienceCurve;
import com.ascension.progression.service.ProgressionService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.tower.service.TowerService;
import java.util.Set;

/** Wires the first playable Phase 9 gameplay services and event bridges. */
public final class PhaseNineModule extends AbstractModule {

    @Override public String id() { return "phase-9"; }
    @Override public Set<String> dependencies() {
        return Set.of("assets", "registry", "profiles", "sessions", "items", "equipment", "effects", "combat", "abilities");
    }

    @Override
    protected void onStart(final ServiceRegistry services) {
        final RegistryHub registries = services.require(RegistryHub.class);
        final ProgressionService progression = new ProgressionService(
            ExperienceCurve.polynomial(100L, 0.15D, 20L), 100
        );
        final TowerService tower = new TowerService(
            registries.require(AscensionRegistries.FLOOR_DEFINITIONS)
        );
        final QuestService quests = new QuestService(
            registries,
            services.require(PlayerProfileService.class),
            progression,
            tower,
            services.require(ItemService.class),
            services.require(ItemMetadataEncoder.class)
        );

        services.register(ProgressionService.class, progression);
        services.register(TowerService.class, tower);
        services.register(QuestService.class, quests);

        final EventBus events = services.require(EventBus.class);
        final GameplayCombatListener listener = new GameplayCombatListener(quests, registries, services);
        events.subscribe("phase-9", com.ascension.combat.event.EntityKilledEvent.class,
            EventPriority.NORMAL, false, listener::onKilled);
        events.subscribe("phase-9", com.ascension.combat.event.DamageAppliedEvent.class,
            EventPriority.MONITOR, false, ignored -> { });
    }

    @Override
    protected void onStop(final ServiceRegistry services) {
        services.find(EventBus.class).ifPresent(events -> events.unsubscribeOwner("phase-9"));
    }
}
