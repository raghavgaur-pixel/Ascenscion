package com.ascension.equipment.runtime;

import com.ascension.equipment.model.EquipmentSlot;
import com.ascension.equipment.model.EquipmentSnapshot;
import com.ascension.equipment.model.EquipmentTransaction;
import com.ascension.items.runtime.AscensionItem;
import java.util.Optional;

/**
 * Runtime container for player equipment.
 */
public interface EquipmentContainer {

    /**
     * Resolves the item in a specific slot.
     *
     * @param slot equipment slot
     * @return equipped item if present
     */
    Optional<AscensionItem> item(EquipmentSlot slot);

    /**
     * @return immutable snapshot of current equipment
     */
    EquipmentSnapshot snapshot();

    /**
     * Starts a transaction to modify equipment.
     *
     * @return new transaction
     */
    EquipmentTransaction startTransaction();
}
