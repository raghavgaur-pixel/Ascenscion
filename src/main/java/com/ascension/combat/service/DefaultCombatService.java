package com.ascension.combat.service;

import com.ascension.combat.event.AttackResolvedEvent;
import com.ascension.combat.event.AttackStartedEvent;
import com.ascension.combat.event.CombatEndedEvent;
import com.ascension.combat.event.CriticalHitEvent;
import com.ascension.combat.event.DamageAppliedEvent;
import com.ascension.combat.event.DamageCalculatedEvent;
import com.ascension.combat.event.EntityKilledEvent;
import com.ascension.combat.model.AttackContext;
import com.ascension.combat.model.CombatContext;
import com.ascension.combat.model.CombatEntity;
import com.ascension.combat.model.CombatSnapshot;
import com.ascension.combat.model.DamageResult;
import com.ascension.combat.model.HitResult;
import com.ascension.events.EventBus;
import com.ascension.stats.attribute.AttributeSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic pipeline orchestrator for combat interactions.
 */
public class DefaultCombatService implements CombatService {

    private final EventBus eventBus;
    private final List<CombatValidator> validators;

    public DefaultCombatService(final EventBus eventBus) {
        this.eventBus = eventBus;
        this.validators = new ArrayList<>();
    }

    @Override
    public void registerValidator(final CombatValidator validator) {
        this.validators.add(validator);
    }

    @Override
    public HitResult execute(final AttackContext attackContext) {
        // 1. Pre-event (Cancellable) - Fired before validation
        final AttackStartedEvent startedEvent = new AttackStartedEvent(attackContext);
        this.eventBus.publish(startedEvent);
        if (startedEvent.cancelled()) {
            return cancelledResult(attackContext);
        }

        // 2. Validation
        for (final CombatValidator validator : this.validators) {
            if (!validator.validate(attackContext)) {
                return cancelledResult(attackContext);
            }
        }

        // 3. Snapshot Creation
        final CombatSnapshot targetSnapshot = createSnapshot(attackContext.target().entity());
        final Optional<CombatSnapshot> attackerSnapshot = attackContext.attacker().map(this::createSnapshot);

        // 4. Combat Context Initialization
        final CombatContext combatContext = new CombatContext(attackContext, attackerSnapshot, targetSnapshot);

        // 5. Damage Calculation Event (Extensible calculation step)
        final DamageCalculatedEvent calculatedEvent = new DamageCalculatedEvent(combatContext);
        this.eventBus.publish(calculatedEvent);

        // 6. Critical Hit Event
        if (combatContext.currentDamageResult().isCritical()) {
            this.eventBus.publish(new CriticalHitEvent(combatContext));
        }

        // Refetch the final damage result after all events that could modify it
        final DamageResult finalResult = combatContext.currentDamageResult();

        // Check if entity was alive before applying damage
        final boolean wasAliveBefore = targetSnapshot.health() > 0;

        // 7. Apply Damage (Health Mutation)
        final CombatEntity targetEntity = attackContext.target().entity();
        targetEntity.health().damage(finalResult.finalDamage());

        // 8. Damage Applied Event
        this.eventBus.publish(new DamageAppliedEvent(combatContext));

        // 9. Entity Killed Event
        boolean wasKilled = false;
        if (wasAliveBefore && !targetEntity.health().isAlive()) {
            wasKilled = true;
            this.eventBus.publish(new EntityKilledEvent(targetEntity, combatContext));
        }

        // 10. Attack Resolved Event
        final HitResult hitResult = new HitResult(attackContext, Optional.of(finalResult), wasKilled, false);
        this.eventBus.publish(new AttackResolvedEvent(combatContext, hitResult));

        // 11. Combat Ended Event
        if (wasKilled) {
            this.eventBus.publish(new CombatEndedEvent(targetEntity));
            // Assuming attacker also formally ends combat here if applicable
            attackContext.attacker().ifPresent(attacker -> this.eventBus.publish(new CombatEndedEvent(attacker)));
        }

        return hitResult;
    }

    private CombatSnapshot createSnapshot(final CombatEntity entity) {
        // AttributeContainer.snapshot() returns AttributeSnapshot which has revision inside it already
        final AttributeSnapshot attributeSnapshot = entity.attributes().snapshot();
        return new CombatSnapshot(
            entity.health().current(),
            entity.health().maximum(),
            attributeSnapshot,
            List.copyOf(entity.effects().snapshot())
        );
    }

    private HitResult cancelledResult(final AttackContext context) {
        return new HitResult(context, Optional.empty(), false, true);
    }
}
