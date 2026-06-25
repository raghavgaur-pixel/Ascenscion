package com.ascension.task;

import com.ascension.core.logging.PluginLogger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit-backed runtime task scheduler with task ownership tracking.
 */
public final class BukkitRuntimeTaskService implements RuntimeTaskService {

    private final JavaPlugin plugin;
    private final PluginLogger logger;
    private final ConcurrentMap<Integer, BukkitRuntimeTaskHandle> activeTasks = new ConcurrentHashMap<>();

    public BukkitRuntimeTaskService(final JavaPlugin plugin, final PluginLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public <T> CompletableFuture<T> runSync(final String owner, final String name, final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this.plugin, () -> executeFuture(owner, name, future, action));
        return future;
    }

    @Override
    public <T> CompletableFuture<T> runAsync(final String owner, final String name, final Supplier<T> action) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> executeFuture(owner, name, future, action));
        return future;
    }

    @Override
    public RuntimeTaskHandle scheduleSyncLater(
        final String owner,
        final String name,
        final long delayTicks,
        final Runnable action
    ) {
        return registerHandle(owner, name, TaskExecutionMode.SYNC, Bukkit.getScheduler().runTaskLater(
            this.plugin,
            () -> {
                try {
                    runGuarded(owner, name, action);
                } finally {
                    removeCompletedTask(owner, name);
                }
            },
            delayTicks
        ));
    }

    @Override
    public RuntimeTaskHandle scheduleAsyncLater(
        final String owner,
        final String name,
        final long delayTicks,
        final Runnable action
    ) {
        return registerHandle(owner, name, TaskExecutionMode.ASYNC, Bukkit.getScheduler().runTaskLaterAsynchronously(
            this.plugin,
            () -> {
                try {
                    runGuarded(owner, name, action);
                } finally {
                    removeCompletedTask(owner, name);
                }
            },
            delayTicks
        ));
    }

    @Override
    public RuntimeTaskHandle scheduleSyncRepeating(
        final String owner,
        final String name,
        final long delayTicks,
        final long periodTicks,
        final Runnable action
    ) {
        return registerHandle(owner, name, TaskExecutionMode.SYNC, Bukkit.getScheduler().runTaskTimer(
            this.plugin,
            () -> runGuarded(owner, name, action),
            delayTicks,
            periodTicks
        ));
    }

    @Override
    public RuntimeTaskHandle scheduleAsyncRepeating(
        final String owner,
        final String name,
        final long delayTicks,
        final long periodTicks,
        final Runnable action
    ) {
        return registerHandle(owner, name, TaskExecutionMode.ASYNC, Bukkit.getScheduler().runTaskTimerAsynchronously(
            this.plugin,
            () -> runGuarded(owner, name, action),
            delayTicks,
            periodTicks
        ));
    }

    @Override
    public void cancelOwner(final String owner) {
        for (final BukkitRuntimeTaskHandle handle : List.copyOf(this.activeTasks.values())) {
            if (handle.owner().equals(owner)) {
                handle.cancel();
            }
        }
    }

    @Override
    public Collection<RuntimeTaskHandle> activeTasks() {
        return this.activeTasks.values().stream()
            .map(handle -> (RuntimeTaskHandle) handle)
            .toList();
    }

    private <T> void executeFuture(
        final String owner,
        final String name,
        final CompletableFuture<T> future,
        final Supplier<T> action
    ) {
        try {
            future.complete(action.get());
        } catch (final Exception exception) {
            this.logger.error("Task '" + owner + ":" + name + "' failed.", exception);
            future.completeExceptionally(exception);
        }
    }

    private void runGuarded(final String owner, final String name, final Runnable action) {
        try {
            action.run();
        } catch (final Exception exception) {
            this.logger.error("Task '" + owner + ":" + name + "' failed.", exception);
        }
    }

    private RuntimeTaskHandle registerHandle(
        final String owner,
        final String name,
        final TaskExecutionMode mode,
        final BukkitTask task
    ) {
        final BukkitRuntimeTaskHandle handle = new BukkitRuntimeTaskHandle(owner, name, mode, task, this.activeTasks);
        this.activeTasks.put(task.getTaskId(), handle);
        return handle;
    }

    private void removeCompletedTask(final String owner, final String name) {
        this.activeTasks.values().removeIf(handle -> handle.owner().equals(owner) && handle.name().equals(name));
    }

    private static final class BukkitRuntimeTaskHandle implements RuntimeTaskHandle {

        private final String owner;
        private final String name;
        private final TaskExecutionMode mode;
        private final BukkitTask task;
        private final ConcurrentMap<Integer, BukkitRuntimeTaskHandle> registry;

        private BukkitRuntimeTaskHandle(
            final String owner,
            final String name,
            final TaskExecutionMode mode,
            final BukkitTask task,
            final ConcurrentMap<Integer, BukkitRuntimeTaskHandle> registry
        ) {
            this.owner = owner;
            this.name = name;
            this.mode = mode;
            this.task = task;
            this.registry = registry;
        }

        @Override
        public String owner() {
            return this.owner;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public TaskExecutionMode mode() {
            return this.mode;
        }

        @Override
        public void cancel() {
            this.task.cancel();
            this.registry.remove(this.task.getTaskId());
        }

        @Override
        public boolean cancelled() {
            return this.task.isCancelled();
        }
    }
}
