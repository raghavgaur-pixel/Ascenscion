package com.ascension.combat.service;

import com.ascension.combat.model.AttackContext;
import com.ascension.combat.model.HitResult;

/**
 * Service for initiating combat attacks through the deterministic pipeline.
 */
public interface CombatService {

    /**
     * Registers a validator that can prevent attacks from occurring.
     *
     * @param validator the validator to register
     */
    void registerValidator(CombatValidator validator);

    /**
     * Executes an attack through the combat pipeline.
     *
     * @param context the requested attack context
     * @return the result of the attack
     */
    HitResult execute(AttackContext context);
}
