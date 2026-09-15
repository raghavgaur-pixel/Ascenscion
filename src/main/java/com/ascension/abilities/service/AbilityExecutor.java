package com.ascension.abilities.service;

import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.abilities.model.AbilityRequest;

/**
 * Executes the gameplay payload of one resolved ability.
 *
 * <p>This boundary is intentionally independent of Paper. Platform-specific
 * effects belong in infrastructure adapters, while deterministic gameplay
 * decisions remain in the application layer.</p>
 */
@FunctionalInterface
public interface AbilityExecutor {

    /**
     * Executes an already validated ability request.
     *
     * @param request original player intent
     * @param definition immutable content definition
     * @return execution result
     */
    AbilityExecutionResult execute(AbilityRequest request, AbilityDefinition definition);
}
