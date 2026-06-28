package com.ascension.equipment.rule;

import com.ascension.equipment.runtime.EquipmentContext;
import com.ascension.validation.ValidationReport;

/**
 * Validation rule for equipment changes.
 */
public interface EquipmentRule {

    /**
     * Validates the equipment context (which may hold pending changes).
     *
     * @param context equipment context
     * @return validation report
     */
    ValidationReport validate(EquipmentContext context);
}
