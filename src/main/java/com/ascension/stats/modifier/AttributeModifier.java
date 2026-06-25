package com.ascension.stats.modifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable runtime attribute modifier instance.
 *
 * @param id unique modifier id
 * @param owner owner module or subsystem
 * @param source stable source descriptor
 * @param category modifier category
 * @param priority priority within the same operation
 * @param operation arithmetic operation
 * @param value modifier value
 * @param appliedAt application time
 * @param expiresAt optional expiration timestamp
 * @param stackingBehavior stacking rule
 * @param removalPolicy removal rule
 */
public record AttributeModifier(
    String id,
    String owner,
    ModifierSource source,
    ModifierCategory category,
    int priority,
    ModifierOperation operation,
    double value,
    Instant appliedAt,
    Optional<Instant> expiresAt,
    ModifierStackingBehavior stackingBehavior,
    ModifierRemovalPolicy removalPolicy
) {

    public AttributeModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(appliedAt, "appliedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(stackingBehavior, "stackingBehavior");
        Objects.requireNonNull(removalPolicy, "removalPolicy");
    }

    /**
     * Creates a permanent modifier.
     *
     * @param id modifier id
     * @param owner owner id
     * @param source source descriptor
     * @param category category
     * @param priority priority
     * @param operation operation
     * @param value value
     * @param stackingBehavior stacking behavior
     * @param removalPolicy removal policy
     * @return modifier instance
     */
    public static AttributeModifier permanent(
        final String id,
        final String owner,
        final ModifierSource source,
        final ModifierCategory category,
        final int priority,
        final ModifierOperation operation,
        final double value,
        final ModifierStackingBehavior stackingBehavior,
        final ModifierRemovalPolicy removalPolicy
    ) {
        return new AttributeModifier(
            id,
            owner,
            source,
            category,
            priority,
            operation,
            value,
            Instant.now(),
            Optional.empty(),
            stackingBehavior,
            removalPolicy
        );
    }

    /**
     * Creates a timed modifier.
     *
     * @param id modifier id
     * @param owner owner id
     * @param source source descriptor
     * @param category category
     * @param priority priority
     * @param operation operation
     * @param value value
     * @param duration duration
     * @param stackingBehavior stacking behavior
     * @param removalPolicy removal policy
     * @return modifier instance
     */
    public static AttributeModifier timed(
        final String id,
        final String owner,
        final ModifierSource source,
        final ModifierCategory category,
        final int priority,
        final ModifierOperation operation,
        final double value,
        final Duration duration,
        final ModifierStackingBehavior stackingBehavior,
        final ModifierRemovalPolicy removalPolicy
    ) {
        final Instant appliedAt = Instant.now();
        return new AttributeModifier(
            id,
            owner,
            source,
            category,
            priority,
            operation,
            value,
            appliedAt,
            Optional.of(appliedAt.plus(duration)),
            stackingBehavior,
            removalPolicy
        );
    }

    /**
     * @param now comparison time
     * @return {@code true} if expired
     */
    public boolean expired(final Instant now) {
        return this.expiresAt.map(expiration -> !expiration.isAfter(now)).orElse(false);
    }
}
