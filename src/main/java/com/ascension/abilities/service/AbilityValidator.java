package com.ascension.abilities.service;

import com.ascension.abilities.model.AbilityRequest;

/**
 * Extension point for deterministic ability preconditions.
 */
@FunctionalInterface
public interface AbilityValidator {

    /**
     * @return a rejection reason, or {@code null} when the request is valid
     */
    String validate(AbilityRequest request);
}
