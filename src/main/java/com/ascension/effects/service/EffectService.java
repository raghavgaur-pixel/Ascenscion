package com.ascension.effects.service;

import com.ascension.assets.model.AssetId;
import com.ascension.effects.runtime.EffectContext;
import com.ascension.effects.runtime.EffectInstance;
import com.ascension.effects.runtime.EffectSource;
import com.ascension.session.model.PlayerSession;

import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrator for the active effects lifecycle.
 */
public interface EffectService {

    /**
     * Applies an effect to a session based on an effect definition.
     *
     * @param session the session to apply the effect to
     * @param effectId the definition ID of the effect
     * @param source where the effect came from
     * @param context contextual parameters
     * @return the applied or updated effect instance, if successful
     */
    Optional<EffectInstance> applyEffect(
        PlayerSession session,
        AssetId effectId,
        EffectSource source,
        EffectContext context
    );

    /**
     * Removes a specific effect instance from a session.
     *
     * @param session the session
     * @param instanceId the runtime UUID of the effect instance
     * @return true if an effect was removed
     */
    boolean removeEffect(PlayerSession session, UUID instanceId);

    /**
     * Attempts to dispel an effect based on rules (to be implemented).
     *
     * @param session the session
     * @param instanceId the runtime UUID of the effect instance
     * @return true if successfully dispelled
     */
    boolean dispelEffect(PlayerSession session, UUID instanceId);

    /**
     * Clears all temporary effects from a session.
     *
     * @param session the session
     */
    void clearEffects(PlayerSession session);
}
