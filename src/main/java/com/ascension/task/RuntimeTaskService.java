package com.ascension.task;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Internal task scheduler abstraction for named and owned tasks.
 */
public interface RuntimeTaskService {

    /**
     * Runs a task immediately on the main thread.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param action task action
     * @return completion future
     * @param <T> result type
     */
    <T> CompletableFuture<T> runSync(String owner, String name, Supplier<T> action);

    /**
     * Runs a task immediately on an async thread.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param action task action
     * @return completion future
     * @param <T> result type
     */
    <T> CompletableFuture<T> runAsync(String owner, String name, Supplier<T> action);

    /**
     * Schedules a sync task after a delay.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param delayTicks delay in ticks
     * @param action task action
     * @return task handle
     */
    RuntimeTaskHandle scheduleSyncLater(String owner, String name, long delayTicks, Runnable action);

    /**
     * Schedules an async task after a delay.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param delayTicks delay in ticks
     * @param action task action
     * @return task handle
     */
    RuntimeTaskHandle scheduleAsyncLater(String owner, String name, long delayTicks, Runnable action);

    /**
     * Schedules a repeating sync task.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param delayTicks initial delay in ticks
     * @param periodTicks repeat period in ticks
     * @param action task action
     * @return task handle
     */
    RuntimeTaskHandle scheduleSyncRepeating(String owner, String name, long delayTicks, long periodTicks, Runnable action);

    /**
     * Schedules a repeating async task.
     *
     * @param owner task owner identifier
     * @param name task name
     * @param delayTicks initial delay in ticks
     * @param periodTicks repeat period in ticks
     * @param action task action
     * @return task handle
     */
    RuntimeTaskHandle scheduleAsyncRepeating(String owner, String name, long delayTicks, long periodTicks, Runnable action);

    /**
     * Cancels every task owned by the given owner.
     *
     * @param owner owner identifier
     */
    void cancelOwner(String owner);

    /**
     * @return immutable snapshot of active tasks
     */
    Collection<RuntimeTaskHandle> activeTasks();
}

