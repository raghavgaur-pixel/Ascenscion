package com.ascension.combat.service;

import com.ascension.combat.model.AttackContext;
import com.ascension.combat.model.CombatEntity;

/**
 * A basic validator that prevents an entity from attacking itself.
 */
public class FriendlyFireValidator implements CombatValidator {

    @Override
    public boolean validate(final AttackContext context) {
        if (context.attacker().isEmpty()) {
            return true;
        }

        final CombatEntity attacker = context.attacker().get();
        final CombatEntity target = context.target().entity();

        // Prevent self-damage
        if (attacker.uniqueId().equals(target.uniqueId())) {
            return false;
        }

        return true;
    }
}
