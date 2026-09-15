package com.ascension.abilities.service;

/**
 * Result returned by a concrete ability executor.
 *
 * <p>Executors never own cooldowns or resource accounting. A rejected result
 * causes the application service to roll back any reserved resources.</p>
 */
public record AbilityExecutionResult(boolean successful, String reason) {

    public AbilityExecutionResult {
        reason = reason == null ? "" : reason;
    }

    public static AbilityExecutionResult success() {
        return new AbilityExecutionResult(true, "");
    }

    public static AbilityExecutionResult rejected(final String reason) {
        return new AbilityExecutionResult(false, reason);
    }
}
