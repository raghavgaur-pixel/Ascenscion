package com.ascension.equipment.service;

import com.ascension.equipment.runtime.EquipmentContainer;
import com.ascension.session.model.PlayerSession;

/**
 * Service for retrieving runtime equipment containers.
 */
public interface EquipmentService {

    /**
     * Retrieves the equipment container for a given session.
     *
     * @param session player session
     * @return equipment container
     */
    EquipmentContainer container(PlayerSession session);
}
