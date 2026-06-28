package com.ascension.equipment.service;

import com.ascension.equipment.rule.EquipmentRuleRegistry;
import com.ascension.equipment.runtime.DefaultEquipmentContainer;
import com.ascension.equipment.runtime.EquipmentContainer;
import com.ascension.events.EventBus;
import com.ascension.session.model.PlayerSession;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultEquipmentService implements EquipmentService {

    private final Map<UUID, EquipmentContainer> containers = new ConcurrentHashMap<>();
    private final EquipmentRuleRegistry ruleRegistry;
    private final EventBus eventBus;

    public DefaultEquipmentService(
        final EquipmentRuleRegistry ruleRegistry,
        final EventBus eventBus
    ) {
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    @Override
    public EquipmentContainer container(final PlayerSession session) {
        return this.containers.computeIfAbsent(
            session.uniqueId(),
            id -> new DefaultEquipmentContainer(session, this.ruleRegistry, this.eventBus)
        );
    }

    public void dispose(final UUID uniqueId) {
        this.containers.remove(uniqueId);
    }
}
