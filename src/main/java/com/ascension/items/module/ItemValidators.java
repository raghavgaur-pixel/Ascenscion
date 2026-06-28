package com.ascension.items.module;

import com.ascension.assets.definition.ItemDefinition;
import com.ascension.validation.DefaultValidationReport;
import com.ascension.validation.Validator;
import java.util.List;

public final class ItemValidators {

    public static final Validator<ItemDefinition> ITEM_DEFINITION = definition -> {
        if (definition == null) {
            throw new IllegalArgumentException("definition is null");
        }
        return new DefaultValidationReport(List.of());
    };

    private ItemValidators() {}
}
