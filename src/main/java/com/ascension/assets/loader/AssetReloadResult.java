package com.ascension.assets.loader;

import com.ascension.validation.ValidationReport;
import java.util.Objects;

/**
 * Result of a reload-safe asset load or reload operation.
 *
 * @param scope logical reload scope
 * @param assetCount number of loaded assets
 * @param report validation report
 */
public record AssetReloadResult(
    String scope,
    int assetCount,
    ValidationReport report
) {

    public AssetReloadResult {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(report, "report");
    }

    /**
     * @return {@code true} when the reload completed without blocking validation issues
     */
    public boolean successful() {
        return !this.report.hasErrors();
    }
}
