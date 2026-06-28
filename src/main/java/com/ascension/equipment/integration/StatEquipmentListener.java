package com.ascension.equipment.integration;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.equipment.event.ItemEquippedEvent;
import com.ascension.equipment.event.ItemUnequippedEvent;
import com.ascension.events.EventListener;
import com.ascension.items.component.StatModifierComponent;
import com.ascension.items.service.ItemService;
import com.ascension.stats.modifier.AttributeModifier;
import com.ascension.stats.modifier.ModifierCategory;
import com.ascension.stats.modifier.ModifierOperation;
import com.ascension.stats.modifier.ModifierRemovalPolicy;
import com.ascension.stats.modifier.ModifierSource;
import com.ascension.stats.modifier.ModifierStackingBehavior;
import java.util.Map;

public final class StatEquipmentListener {

    private final ItemService itemService;

    public StatEquipmentListener(final ItemService itemService) {
        this.itemService = itemService;
    }

    public EventListener<ItemEquippedEvent> onEquip() {
        return event -> {
            this.itemService.findDefinition(event.item().definitionId()).ifPresent(definition -> {
                definition.component(StatModifierComponent.class).ifPresent(statComponent -> {
                    for (final Map.Entry<AssetId, Double> stat : statComponent.modifiers().entrySet()) {
                        final ModifierSource source = new ModifierSource("equipment", "item", event.item().instanceId().toString());
                        final AttributeModifier modifier = AttributeModifier.permanent(
                            event.item().instanceId().toString() + "_" + stat.getKey().toString(),
                            "equipment",
                            source,
                            ModifierCategory.EQUIPMENT,
                            0,
                            ModifierOperation.FLAT,
                            stat.getValue(),
                            ModifierStackingBehavior.STACK,
                            ModifierRemovalPolicy.MANUAL
                        );
                        event.session().attributes().addModifier(stat.getKey(), modifier);
                    }
                });
            });
        };
    }

    public EventListener<ItemUnequippedEvent> onUnequip() {
        return event -> {
            final String sourceId = event.item().instanceId().toString();
            event.session().attributes().removeSource(sourceId);
        };
    }
}
