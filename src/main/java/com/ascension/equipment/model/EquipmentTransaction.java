package com.ascension.equipment.model;

import com.ascension.items.runtime.AscensionItem;
import com.ascension.validation.ValidationReport;

/**
 * Transaction for atomic equipment changes.
 */
public interface EquipmentTransaction {

    /**
     * Equips an item to a slot.
     *
     * @param slot slot to equip
     * @param item item to equip
     * @return transaction for chaining
     */
    EquipmentTransaction equip(EquipmentSlot slot, AscensionItem item);

    /**
     * Unequips an item from a slot.
     *
     * @param slot slot to unequip
     * @return transaction for chaining
     */
    EquipmentTransaction unequip(EquipmentSlot slot);

    /**
     * Validates and applies the transaction.
     *
     * @return validation report, returning errors if rules were violated
     */
    ValidationReport commit();

    /**
     * Reverts all pending operations in this transaction.
     */
    void rollback();
}
