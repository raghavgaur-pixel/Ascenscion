package com.ascension.equipment.runtime;

import com.ascension.session.model.PlayerSession;
import java.util.Objects;

/**
 * Context passed to rules and transactions containing the session and pending equipment state.
 *
 * @param session player session
 * @param container view of the equipment container (often pending state during a transaction)
 */
public record EquipmentContext(
    PlayerSession session,
    EquipmentContainer container
) {
    public EquipmentContext {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(container, "container");
    }
}
