package com.ascension.items.meta;

import com.ascension.assets.model.AssetId;
import com.ascension.items.runtime.AscensionItem;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitItemMetadataEncoder implements ItemMetadataEncoder {

    private final NamespacedKey definitionIdKey;
    private final NamespacedKey instanceIdKey;

    public BukkitItemMetadataEncoder(JavaPlugin plugin) {
        this.definitionIdKey = new NamespacedKey(plugin, "ascension_item_id");
        this.instanceIdKey = new NamespacedKey(plugin, "ascension_instance_id");
    }

    @Override
    public void encode(AscensionItem item, Object platformItem) {
        if (!(platformItem instanceof ItemStack itemStack)) {
            throw new IllegalArgumentException("Platform item must be a Bukkit ItemStack");
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.definitionIdKey, PersistentDataType.STRING, item.definitionId().toString());
        container.set(this.instanceIdKey, PersistentDataType.STRING, item.instanceId().toString());
        itemStack.setItemMeta(meta);
    }

    @Override
    public Optional<AssetId> decodeDefinitionId(Object platformItem) {
        if (!(platformItem instanceof ItemStack itemStack)) {
            return Optional.empty();
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String rawId = container.get(this.definitionIdKey, PersistentDataType.STRING);
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(AssetId.parse(rawId));
    }

    @Override
    public Optional<UUID> decodeInstanceId(Object platformItem) {
        if (!(platformItem instanceof ItemStack itemStack)) {
            return Optional.empty();
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String rawId = container.get(this.instanceIdKey, PersistentDataType.STRING);
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
