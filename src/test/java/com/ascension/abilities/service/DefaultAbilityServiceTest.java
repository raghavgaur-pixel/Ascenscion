package com.ascension.abilities.service;

import com.ascension.abilities.model.AbilityCost;
import com.ascension.abilities.model.AbilityRequest;
import com.ascension.abilities.model.AbilityResult;
import com.ascension.abilities.model.AbilityTargetType;
import com.ascension.assets.definition.AbilityDefinition;
import com.ascension.assets.model.AssetCompatibility;
import com.ascension.assets.model.AssetDescriptor;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.SemanticVersion;
import com.ascension.registry.ConcurrentMutableRegistry;
import com.ascension.registry.RegistryDescriptor;
import com.ascension.serialization.SerializedObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAbilityServiceTest {

    private static final AssetId SLASH = AssetId.parse("ascension:slash");

    @Test
    void successfulExecutionConsumesResourcesAndStartsCooldown() {
        final TestResources resources = new TestResources();
        final CooldownTracker cooldowns = new CooldownTracker();
        final var definition = definition(1_000L, Map.of("mana", 25.0D));
        final var registry = registry(definition);
        final var executors = new AbilityExecutorRegistry();
        executors.register(SLASH, (request, resolved) -> AbilityExecutionResult.success());
        final AtomicLong now = new AtomicLong(10_000L);
        final var service = new DefaultAbilityService(
            registry, executors, resources, cooldowns, now::get
        );

        final UUID actor = UUID.randomUUID();
        resources.balances.put(actor, 100.0D);
        final AbilityResult first = service.execute(new AbilityRequest(actor, SLASH.toString(), null));

        assertEquals(AbilityResult.Status.SUCCESS, first.status());
        assertEquals(75.0D, resources.balances.get(actor));
        assertTrue(resources.consumeCalls == 1);

        final AbilityResult second = service.execute(new AbilityRequest(actor, SLASH.toString(), null));
        assertEquals(AbilityResult.Status.REJECTED, second.status());
        assertTrue(second.reason().contains("cooldown"));
        assertEquals(75.0D, resources.balances.get(actor));
    }

    @Test
    void failedExecutionRefundsResourcesAndLeavesCooldownReady() {
        final TestResources resources = new TestResources();
        final var definition = definition(1_000L, Map.of("mana", 25.0D));
        final var registry = registry(definition);
        final var executors = new AbilityExecutorRegistry();
        executors.register(SLASH, (request, resolved) -> AbilityExecutionResult.rejected("target invalid"));
        final AtomicLong now = new AtomicLong(10_000L);
        final var service = new DefaultAbilityService(
            registry, executors, resources, new CooldownTracker(), now::get
        );

        final UUID actor = UUID.randomUUID();
        resources.balances.put(actor, 100.0D);
        final AbilityResult result = service.execute(new AbilityRequest(actor, SLASH.toString(), null));

        assertEquals(AbilityResult.Status.REJECTED, result.status());
        assertEquals("target invalid", result.reason());
        assertEquals(100.0D, resources.balances.get(actor));
        assertEquals(1, resources.refundCalls);
    }

    @Test
    void validatorRunsBeforeResourcesAndExecution() {
        final TestResources resources = new TestResources();
        final var registry = registry(definition(0L, Map.of("mana", 10.0D)));
        final var executors = new AbilityExecutorRegistry();
        final boolean[] executed = {false};
        executors.register(SLASH, (request, resolved) -> {
            executed[0] = true;
            return AbilityExecutionResult.success();
        });
        final var service = new DefaultAbilityService(
            registry, executors, resources, new CooldownTracker(), () -> 1_000L
        );
        service.registerValidator(request -> "silenced");

        final UUID actor = UUID.randomUUID();
        resources.balances.put(actor, 100.0D);
        final AbilityResult result = service.execute(new AbilityRequest(actor, SLASH.toString(), null));

        assertEquals(AbilityResult.Status.REJECTED, result.status());
        assertEquals("silenced", result.reason());
        assertFalse(executed[0]);
        assertEquals(0, resources.consumeCalls);
    }

    private static AbilityDefinition definition(final long cooldownMillis, final Map<String, Double> costs) {
        return new AbilityDefinition(
            new AssetDescriptor(
                SLASH,
                "core",
                new SemanticVersion(1, 0, 0),
                "Slash",
                "A test ability.",
                Map.of(),
                AssetCompatibility.open(),
                java.util.Set.of()
            ),
            AbilityTargetType.NONE,
            new AbilityCost(costs),
            cooldownMillis,
            SerializedObject.empty()
        );
    }

    private static ConcurrentMutableRegistry<AssetId, AbilityDefinition> registry(
        final AbilityDefinition definition
    ) {
        final var registry = new ConcurrentMutableRegistry<>(
            new RegistryDescriptor<>("abilities", AssetId.class, AbilityDefinition.class)
        );
        registry.register(definition.id(), definition);
        return registry;
    }

    private static final class TestResources implements ResourceGateway {
        private final Map<UUID, Double> balances = new HashMap<>();
        private int consumeCalls;
        private int refundCalls;

        @Override
        public synchronized boolean consume(final UUID actorId, final Map<String, Double> costs) {
            consumeCalls++;
            final double total = costs.getOrDefault("mana", 0.0D);
            final double balance = balances.getOrDefault(actorId, 0.0D);
            if (balance < total) {
                return false;
            }
            balances.put(actorId, balance - total);
            return true;
        }

        @Override
        public synchronized void refund(final UUID actorId, final Map<String, Double> costs) {
            refundCalls++;
            final double total = costs.getOrDefault("mana", 0.0D);
            balances.merge(actorId, total, Double::sum);
        }
    }
}
