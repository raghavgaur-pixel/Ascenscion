package com.ascension.abilities.model;

import java.util.Objects;

/**
 * Outcome of an ability execution request.
 */
public record AbilityResult(Status status, String reason) {

    public AbilityResult {
        Objects.requireNonNull(status, "status");
        reason = reason == null ? "" : reason;
    }

    public static AbilityResult success() {
        return new AbilityResult(Status.SUCCESS, "");
    }

    public static AbilityResult rejected(final String reason) {
        return new AbilityResult(Status.REJECTED, reason);
    }

    public enum Status {
        SUCCESS,
        REJECTED
    }
}
