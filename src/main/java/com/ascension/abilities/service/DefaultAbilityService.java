package com.ascension.abilities.service;

import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.model.AbilityResult;
import com.ascension.abilities.model.AbilityTargetType;
import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Default transactional ability application service.
 *
 * <p>The service deliberately contains orchestration only. Content definitions,
 * resource accounting and concrete behaviors are supplied through explicit
 * boundaries, keeping the engine testable and extension-friendly.</p>
 */
public final class DefaultAbilityService implements AbilityService {

    private final Registry<AssetId, AbilityDefinition> definitions;
    private final AbilityExecutorRegistry executors;
    private final ResourceGateway resources;
    private final CooldownTracker cooldowns;
    private final LongSupplier clockMillis;
    private final List<AbilityValidator> validators = new ArrayList<>();
    private final Object[] actorLocks = new Object[64];

    public DefaultAbilityService(
        final Registry<AssetId, AbilityDefinition> definitions,
        final AbilityExecutorRegistry executors,
        final ResourceGateway resources,
        final CooldownTracker cooldowns
    ) {
        this(definitions, executors, resources, cooldowns, System::currentTimeMillis);
    }

    public DefaultAbilityService(
        final Registry<AssetId, AbilityDefinition> definitions,
        final AbilityExecutorRegistry executors,
        final ResourceGateway resources,
        final CooldownTracker cooldowns,
        final LongSupplier clockMillis
    ) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.executors = Objects.requireNonNull(executors, "executors");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
        for (int index = 0; index < this.actorLocks.length; index++) {
            this.actorLocks[index] = new Object();
        }
    }

    @Override
    public AbilityResult execute(final AbilityRequest request) {
        Objects.requireNonNull(request, "request");

        final AssetId abilityId;
        try {
            abilityId = AssetId.parse(request.abilityId());
        } catch (final IllegalArgumentException exception) {
            return AbilityResult.rejected("Invalid ability id: " + request.abilityId());
        }

        final Optional<AbilityDefinition> optionalDefinition = this.definitions.find(abilityId);
        if (optionalDefinition.isEmpty()) {
            return AbilityResult.rejected("Unknown ability: " + abilityId);
        }

        final AbilityDefinition definition = optionalDefinition.get();
        final String validatorFailure = this.validate(request, definition);
        if (validatorFailure != null) {
            return AbilityResult.rejected(validatorFailure);
        }

        final AbilityExecutor executor = this.executors.find(abilityId).orElse(null);
        if (executor == null) {
            return AbilityResult.rejected("Ability behavior is not registered: " + abilityId);
        }

        synchronized (this.lockFor(request.actorId())) {
            final long now = this.clockMillis.getAsLong();
            if (now < 0L) {
                throw new IllegalStateException("Clock returned a negative timestamp");
            }

            final long remaining = this.cooldowns.remainingMillis(request.actorId(), abilityId, now);
            if (remaining > 0L) {
                return AbilityResult.rejected("Ability is on cooldown for " + remaining + "ms");
            }

            final var cost = definition.cost().resources();
            if (!this.resources.consume(request.actorId(), cost)) {
                return AbilityResult.rejected("Insufficient resources");
            }

            final AbilityExecutionResult execution;
            try {
                execution = Objects.requireNonNull(
                    executor.execute(request, definition),
                    "ability executor result"
                );
            } catch (final RuntimeException exception) {
                this.resources.refund(request.actorId(), cost);
                return AbilityResult.rejected("Ability execution failed");
            }

            if (!execution.successful()) {
                this.resources.refund(request.actorId(), cost);
                return AbilityResult.rejected(execution.reason());
            }

            try {
                this.cooldowns.tryAcquire(
                    request.actorId(),
                    abilityId,
                    now,
                    definition.cooldownMillis()
                );
            } catch (final ArithmeticException exception) {
                this.resources.refund(request.actorId(), cost);
                return AbilityResult.rejected("Ability cooldown overflow");
            }

            return AbilityResult.success();
        }
    }

    @Override
    public void registerValidator(final AbilityValidator validator) {
        this.validators.add(Objects.requireNonNull(validator, "validator"));
    }

    @Override
    public Optional<AbilityDefinition> definition(final String abilityId) {
        try {
            return this.definitions.find(AssetId.parse(abilityId));
        } catch (final IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * Exposes runtime cleanup without coupling the service to player sessions.
     */
    public void clearRuntime(final UUID actorId) {
        this.cooldowns.clear(Objects.requireNonNull(actorId, "actorId"));
    }

    private String validate(final AbilityRequest request, final AbilityDefinition definition) {
        final AbilityTargetType targetType = definition.targetType();
        final boolean hasEntityTarget = request.targetId() != null;
        final boolean hasPointTarget = request.targetPoint() != null;

        switch (targetType) {
            case SINGLE_ENTITY -> {
                if (!hasEntityTarget) {
                    return "A target entity is required";
                }
                if (hasPointTarget) {
                    return "Single-entity abilities cannot receive a point target";
                }
            }
            case GROUND -> {
                if (!hasPointTarget) {
                    return "A ground position is required";
                }
                if (hasEntityTarget) {
                    return "Ground abilities cannot receive an entity target";
                }
            }
            case AREA, CONE, LINE, PROJECTILE -> {
                if (!hasEntityTarget && !hasPointTarget) {
                    return "An entity or point target is required";
                }
            }
            case SELF -> {
                if (hasPointTarget) {
                    return "Self abilities cannot receive a point target";
                }
            }
            case NONE -> {
                if (hasEntityTarget || hasPointTarget) {
                    return "This ability does not accept a target";
                }
            }
        }

        for (final AbilityValidator validator : List.copyOf(this.validators)) {
            final String failure = validator.validate(request);
            if (failure != null && !failure.isBlank()) {
                return failure;
            }
        }
        return null;
    }

    private Object lockFor(final UUID actorId) {
        return this.actorLocks[(actorId.hashCode() & Integer.MAX_VALUE) % this.actorLocks.length];
    }
}
