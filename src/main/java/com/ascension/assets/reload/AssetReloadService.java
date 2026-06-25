package com.ascension.assets.reload;

import com.ascension.assets.loader.AssetReloadResult;

/**
 * Hot reload service for asset groups and typed configuration ownership.
 */
public interface AssetReloadService {

    /**
     * Reloads every registered asset group.
     */
    AssetReloadResult reloadAllAssets();

    /**
     * Reloads a single asset group.
     *
     * @param assetTypeId asset group id
     */
    AssetReloadResult reloadAssetGroup(String assetTypeId);

    /**
     * Reloads every typed configuration owned by a module.
     *
     * @param owner owner identifier
     */
    void reloadOwnedConfiguration(String owner);
}
