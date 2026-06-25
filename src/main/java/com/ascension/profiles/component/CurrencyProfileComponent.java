package com.ascension.profiles.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player currencies.
 */
public final class CurrencyProfileComponent implements ProfileComponent {

    private final Map<String, Long> balances;

    public CurrencyProfileComponent() {
        this.balances = new ConcurrentHashMap<>();
    }

    public CurrencyProfileComponent(final Map<String, Long> balances) {
        this.balances = new ConcurrentHashMap<>(balances);
    }

    public long balance(final String currencyId) {
        return this.balances.getOrDefault(currencyId, 0L);
    }

    public void set(final String currencyId, final long amount) {
        this.balances.put(currencyId, amount);
    }

    public long add(final String currencyId, final long delta) {
        return this.balances.merge(currencyId, delta, Long::sum);
    }

    public Map<String, Long> snapshot() {
        return Map.copyOf(this.balances);
    }
}

