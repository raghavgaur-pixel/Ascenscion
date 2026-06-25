package com.ascension.events;

import com.ascension.core.logging.PluginLogger;
import com.ascension.task.RuntimeTaskService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe internal event bus with priority ordering and exception isolation.
 */
public final class DefaultEventBus implements EventBus {

    private final PluginLogger logger;
    private final RuntimeTaskService taskService;
    private final CopyOnWriteArrayList<RegisteredListener<?>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong registrationOrder = new AtomicLong();

    public DefaultEventBus(final PluginLogger logger, final RuntimeTaskService taskService) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.taskService = Objects.requireNonNull(taskService, "taskService");
    }

    @Override
    public <T extends AscensionEvent> EventSubscription subscribe(
        final String owner,
        final Class<T> eventType,
        final EventPriority priority,
        final boolean receiveCancelled,
        final EventListener<T> listener
    ) {
        final RegisteredListener<T> registeredListener = new RegisteredListener<>(
            Objects.requireNonNull(owner, "owner"),
            Objects.requireNonNull(eventType, "eventType"),
            Objects.requireNonNull(priority, "priority"),
            receiveCancelled,
            Objects.requireNonNull(listener, "listener"),
            this.registrationOrder.incrementAndGet()
        );
        this.listeners.add(registeredListener);
        return () -> this.listeners.remove(registeredListener);
    }

    @Override
    public <T extends AscensionEvent> T publish(final T event) {
        Objects.requireNonNull(event, "event");

        final List<RegisteredListener<?>> applicableListeners = new ArrayList<>();
        for (final RegisteredListener<?> registeredListener : this.listeners) {
            if (registeredListener.eventType().isAssignableFrom(event.getClass())) {
                applicableListeners.add(registeredListener);
            }
        }

        applicableListeners.sort(
            Comparator.comparing((RegisteredListener<?> listener) -> listener.priority().ordinal())
                .thenComparingLong(RegisteredListener::registrationOrder)
        );

        for (final RegisteredListener<?> registeredListener : applicableListeners) {
            if (event instanceof CancellableEvent cancellableEvent
                && cancellableEvent.cancelled()
                && !registeredListener.receiveCancelled()) {
                continue;
            }

            try {
                dispatch(registeredListener, event);
            } catch (final Exception exception) {
                this.logger.error(
                    "Event listener failed for owner '" + registeredListener.owner() + "' and event '"
                        + event.getClass().getSimpleName() + "'.",
                    exception
                );
            }
        }

        return event;
    }

    @Override
    public <T extends AscensionEvent> CompletableFuture<T> publishAsync(final T event) {
        return this.taskService.runAsync("runtime-engine", "event-" + event.getClass().getSimpleName(), () -> this.publish(event))
            .thenApply(ignored -> event);
    }

    @Override
    public void unsubscribeOwner(final String owner) {
        this.listeners.removeIf(listener -> listener.owner().equals(owner));
    }

    @SuppressWarnings("unchecked")
    private static <T extends AscensionEvent> void dispatch(
        final RegisteredListener<?> registeredListener,
        final T event
    ) {
        ((RegisteredListener<T>) registeredListener).listener().handle(event);
    }

    private record RegisteredListener<T extends AscensionEvent>(
        String owner,
        Class<T> eventType,
        EventPriority priority,
        boolean receiveCancelled,
        EventListener<T> listener,
        long registrationOrder
    ) {
    }
}

