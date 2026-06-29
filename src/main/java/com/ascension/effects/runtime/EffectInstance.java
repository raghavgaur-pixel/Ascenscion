package com.ascension.effects.runtime;

import com.ascension.assets.model.AssetId;
import com.ascension.effects.definition.EffectDefinition;
import com.ascension.effects.model.EffectDurationType;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Runtime representation of an active effect on a player session.
 */
public final class EffectInstance {

    private final UUID instanceId;
    private final AssetId effectId;
    private final EffectDefinition definition;
    private final EffectSource source;
    private final EffectContext context;
    private final Instant appliedAt;

    private volatile int currentStacks;
    private volatile Optional<Instant> expiresAt;
    private volatile Optional<Duration> activeDuration;

    public EffectInstance(
        final AssetId effectId,
        final EffectDefinition definition,
        final EffectSource source,
        final EffectContext context,
        final Instant appliedAt,
        final Optional<Instant> expiresAt,
        final int initialStacks
    ) {
        this.instanceId = UUID.randomUUID();
        this.effectId = Objects.requireNonNull(effectId, "effectId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.source = Objects.requireNonNull(source, "source");
        this.context = Objects.requireNonNull(context, "context");
        this.appliedAt = Objects.requireNonNull(appliedAt, "appliedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.currentStacks = initialStacks;
    }

    public UUID instanceId() {
        return this.instanceId;
    }

    public AssetId effectId() {
        return this.effectId;
    }

    public EffectDefinition definition() {
        return this.definition;
    }

    public EffectSource source() {
        return this.source;
    }

    public EffectContext context() {
        return this.context;
    }

    public Instant appliedAt() {
        return this.appliedAt;
    }

    public int currentStacks() {
        return this.currentStacks;
    }

    public void setStacks(final int stacks) {
        this.currentStacks = stacks;
    }

    public Optional<Instant> expiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(final Optional<Instant> expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Checks if the effect has expired.
     * @param now The current time to check against.
     * @return true if the effect is timed and the expiration time has passed.
     */
    public boolean isExpired(final Instant now) {
        if (this.definition.durationType() == EffectDurationType.INFINITE) {
            return false;
        }
        return this.expiresAt.map(expiration -> !expiration.isAfter(now)).orElse(false);
    }
}
