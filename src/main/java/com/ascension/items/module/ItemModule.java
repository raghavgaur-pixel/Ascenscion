package com.ascension.items.module;

import com.ascension.assets.loader.AssetService;
import com.ascension.assets.loader.AssetType;
import com.ascension.core.module.AbstractModule;
import com.ascension.core.service.ServiceRegistry;
import com.ascension.items.definition.ItemDefinitionSerializer;
import com.ascension.items.service.DefaultItemService;
import com.ascension.items.service.ItemService;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.RegistryHub;
import com.ascension.items.meta.ItemMetadataEncoder;
import com.ascension.items.meta.BukkitItemMetadataEncoder;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemModule extends AbstractModule {

    @Override
    public String id() {
        return "items";
    }

    @Override
    public java.util.Set<String> dependencies() {
        return java.util.Set.of("assets", "registry");
    }

    @Override
    protected void onStart(ServiceRegistry services) {
        final AssetService assetService = services.require(AssetService.class);
        assetService.registerType(new AssetType<>(
            "items",
            "items",
            "items",
            AscensionRegistries.ITEM_DEFINITIONS,
            new ItemDefinitionSerializer(),
            ItemValidators.ITEM_DEFINITION
        ));

        services.register(ItemService.class, new DefaultItemService(services.require(RegistryHub.class)));
        services.register(ItemMetadataEncoder.class, new BukkitItemMetadataEncoder(services.require(JavaPlugin.class)));
    }

    @Override
    protected void onStop(ServiceRegistry services) {
    }
}
