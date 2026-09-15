package com.ascension.abilities.service;

import java.util.Map;
import java.util.UUID;

/**
 * Atomic resource accounting boundary used by ability execution.
 *
 * <p>The ability engine only knows resource identifiers and amounts. Mana,
 * stamina, health, charges, or future custom resources can therefore be
 * implemented by different progression/runtime layers without changing the
 * ability API.</p>
 */
public interface ResourceGateway {

    /**
     * Attempts to atomically consume all requested resources.
     *
     * @return {@code true} when every resource was reserved/consumed
     */
    boolean consume(UUID actorId, Map<String, Double> resources);

    /**
     * Returns previously consumed resources after an execution rollback.
     */
    void refund(UUID actorId, Map<String, Double> resources);

    /**
     * No-op gateway useful for abilities with no costs and isolated tests.
     */
    static ResourceGateway noOp() {
        return new ResourceGateway() {
            @Override
            public boolean consume(final UUID actorId, final Map<String, Double> resources) {
                return true;
            }

            @Override
            public void refund(final UUID actorId, final Map<String, Double> resources) {
                // Nothing to refund.
            }
        };
    }
}
