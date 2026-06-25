package com.ascension.runtime.tick;

import com.ascension.core.logging.PluginLogger;
import com.ascension.task.RuntimeTaskHandle;
import com.ascension.task.RuntimeTaskService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-task centralized game loop with ordered execution and metrics.
 */
public final class DefaultGameLoop implements GameLoop {

    private static final long WARNING_THRESHOLD_NANOS = 5_000_000L;

    private final PluginLogger logger;
    private final RuntimeTaskService taskService;
    private final CopyOnWriteArrayList<RegisteredTickTask> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, MutableTickMetrics> metrics = new ConcurrentHashMap<>();
    private final AtomicLong registrationSequence = new AtomicLong();
    private volatile RuntimeTaskHandle loopHandle;
    private volatile long tickCounter;

    public DefaultGameLoop(final PluginLogger logger, final RuntimeTaskService taskService) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.taskService = Objects.requireNonNull(taskService, "taskService");
    }

    @Override
    public synchronized void start() {
        if (this.loopHandle != null) {
            throw new IllegalStateException("Game loop is already running.");
        }

        this.loopHandle = this.taskService.scheduleSyncRepeating(
            "runtime-engine",
            "game-loop",
            1L,
            1L,
            this::tick
        );
    }

    @Override
    public synchronized void stop() {
        if (this.loopHandle != null) {
            this.loopHandle.cancel();
            this.loopHandle = null;
        }
        this.tasks.clear();
    }

    @Override
    public TickRegistration register(final TickTask task) {
        final RegisteredTickTask registered = new RegisteredTickTask(task, this.registrationSequence.incrementAndGet());
        this.tasks.add(registered);
        return () -> this.tasks.remove(registered);
    }

    @Override
    public void unregisterOwner(final String owner) {
        this.tasks.removeIf(task -> task.task().owner().equals(owner));
    }

    @Override
    public Collection<TickMetrics> metrics() {
        return this.metrics.values().stream()
            .map(MutableTickMetrics::snapshot)
            .toList();
    }

    private void tick() {
        final TickContext context = new TickContext(++this.tickCounter, System.currentTimeMillis());
        final List<RegisteredTickTask> orderedTasks = new ArrayList<>(this.tasks);
        orderedTasks.sort(
            Comparator.comparing((RegisteredTickTask task) -> task.task().priority().ordinal())
                .thenComparingLong(RegisteredTickTask::registrationOrder)
        );

        for (final RegisteredTickTask registeredTask : orderedTasks) {
            final long startedAt = System.nanoTime();
            try {
                registeredTask.task().tickable().tick(context);
            } catch (final Exception exception) {
                this.logger.error(
                    "Tick task '" + registeredTask.task().owner() + ":" + registeredTask.task().name() + "' failed.",
                    exception
                );
            } finally {
                final long duration = System.nanoTime() - startedAt;
                this.metrics.computeIfAbsent(metricKey(registeredTask.task()), ignored ->
                    new MutableTickMetrics(registeredTask.task().owner(), registeredTask.task().name())
                ).record(duration);

                if (duration >= WARNING_THRESHOLD_NANOS) {
                    this.logger.warn(
                        "Tick task '" + registeredTask.task().owner() + ":" + registeredTask.task().name()
                            + "' took " + duration + "ns."
                    );
                }
            }
        }
    }

    private static String metricKey(final TickTask task) {
        return task.owner() + ":" + task.name();
    }

    private record RegisteredTickTask(TickTask task, long registrationOrder) {
    }

    private static final class MutableTickMetrics {

        private final String owner;
        private final String name;
        private long invocations;
        private long lastDurationNanos;
        private long maxDurationNanos;

        private MutableTickMetrics(final String owner, final String name) {
            this.owner = owner;
            this.name = name;
        }

        private synchronized void record(final long durationNanos) {
            this.invocations++;
            this.lastDurationNanos = durationNanos;
            this.maxDurationNanos = Math.max(this.maxDurationNanos, durationNanos);
        }

        private synchronized TickMetrics snapshot() {
            return new TickMetrics(
                this.owner,
                this.name,
                this.invocations,
                this.lastDurationNanos,
                this.maxDurationNanos
            );
        }
    }
}

