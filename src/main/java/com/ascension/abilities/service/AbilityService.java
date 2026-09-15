package com.ascension.abilities.service;

import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.model.AbilityResult;
import java.util.Optional;

/**
 * Application boundary for ability execution.
 *
 * <p>Implementations are responsible for resolving definitions, validating
 * requests, enforcing cooldowns/costs, and delegating deterministic effects to
 * the appropriate gameplay services.</p>
 */
public interface AbilityService {

    /**
     * Executes an ability request.
     */
    AbilityResult execute(AbilityRequest request);

    /**
     * Registers an execution validator.
     */
    void registerValidator(AbilityValidator validator);

    /**
     * Resolves an ability definition by its stable asset identifier.
     */
    Optional<AbilityDefinition> definition(String abilityId);
}
