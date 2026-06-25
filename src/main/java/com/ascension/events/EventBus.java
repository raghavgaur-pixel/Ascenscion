package com.ascension.events;

import java.util.concurrent.CompletableFuture;

/**
 * Internal event bus used by Ascension systems.
 */
public interface EventBus {

    /**
     * Registers a listener.
     *
     * @param owner owner identifier, typically a module id
     * @param eventType event type
     * @param priority listener priority
     * @param receiveCancelled whether cancelled events should still be delivered
     * @param listener listener callback
     * @param <T> event type
     * @return subscription handle
     */
    <T extends AscensionEvent> EventSubscription subscribe(
        String owner,
        Class<T> eventType,
        EventPriority priority,
        boolean receiveCancelled,
        EventListener<T> listener
    );

    /**
     * Publishes an event on the current thread.
     *
     * @param event event instance
     * @param <T> event type
     * @return same event instance
     */
    <T extends AscensionEvent> T publish(T event);

    /**
     * Publishes an event asynchronously.
     *
     * @param event event instance
     * @param <T> event type
     * @return async completion with the same event instance
     */
    <T extends AscensionEvent> CompletableFuture<T> publishAsync(T event);

    /**
     * Unsubscribes every listener owned by the given owner.
     *
     * @param owner owner identifier
     */
    void unsubscribeOwner(String owner);
}

