package com.ascension.equipment.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe registry for equipment rules.
 */
public final class EquipmentRuleRegistry {

    private final List<EquipmentRule> rules = new ArrayList<>();

    public synchronized void register(final EquipmentRule rule) {
        this.rules.add(rule);
    }

    public synchronized List<EquipmentRule> rules() {
        return List.copyOf(this.rules);
    }
}
