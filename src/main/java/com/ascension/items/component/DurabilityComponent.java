package com.ascension.items.component;

public record DurabilityComponent(int maxDurability) implements ItemComponent {
    @Override
    public String type() {
        return "durability";
    }
}
