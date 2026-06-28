package com.ascension.items.component;

import com.ascension.assets.model.AssetId;
import java.util.Map;

public record StatModifierComponent(Map<AssetId, Double> modifiers) implements ItemComponent {
    @Override
    public String type() {
        return "stat_modifiers";
    }
}
