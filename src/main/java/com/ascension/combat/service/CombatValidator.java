package com.ascension.combat.service;

import com.ascension.combat.model.AttackContext;

/**
 * Validates whether a combat request is allowed to proceed.
 * Future systems should register validators to enforce PvP rules, safe zones, etc.
 */
public interface CombatValidator {

    /**
     * @param context the context to validate
     * @return true if the attack should be allowed
     */
    boolean validate(AttackContext context);
}
