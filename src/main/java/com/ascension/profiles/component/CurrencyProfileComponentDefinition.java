package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;

/**
 * Definition for player currencies.
 */
public final class CurrencyProfileComponentDefinition implements ProfileComponentDefinition<CurrencyProfileComponent> {

    @Override
    public String id() {
        return "currencies";
    }

    @Override
    public Class<CurrencyProfileComponent> type() {
        return CurrencyProfileComponent.class;
    }

    @Override
    public CurrencyProfileComponent createDefault(final ProfileLoadRequest request) {
        return new CurrencyProfileComponent();
    }

    @Override
    public SerializedObject serialize(final CurrencyProfileComponent component) {
        return SerializedObject.builder()
            .put("balances", component.snapshot())
            .build();
    }

    @Override
    public CurrencyProfileComponent deserialize(final SerializedObject data) {
        return new CurrencyProfileComponent(data.getLongMap("balances"));
    }
}

