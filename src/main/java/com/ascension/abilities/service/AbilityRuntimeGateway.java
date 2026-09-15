package com.ascension.abilities.service;

import com.ascension.assets.model.AssetId;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.HitResult;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectInstance;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.session.model.PlayerSession;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Runtime boundary used by authored ability behaviors. */
public interface AbilityRuntimeGateway {

    Optional<CombatEntity> combatEntity(UUID uniqueId);

    Collection<CombatEntity> nearbyCombatEntities(UUID originId, double radius);

    Optional<PlayerSession> playerSession(UUID uniqueId);

    HitResult attack(CombatEntity attacker, CombatEntity target, AssetId abilityId, double damage);

    Optional<EffectInstance> applyEffect(
        PlayerSession target,
        AssetId effectId,
        EffectSource source,
        EffectContext context
    );
}
