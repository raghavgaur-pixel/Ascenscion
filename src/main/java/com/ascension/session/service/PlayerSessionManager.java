package com.ascension.session.service;

import com.ascension.session.model.PlayerSession;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/**
 * Runtime service for online player session lifecycle.
 */
public interface PlayerSessionManager {

    /**
     * Starts session loading for a joining player.
     *
     * @param player joining player
     * @return async completion with the created session
     */
    CompletableFuture<PlayerSession> handleJoin(Player player);

    /**
     * Starts session teardown for a leaving player.
     *
     * @param uniqueId leaving player uuid
     * @return async completion
     */
    CompletableFuture<Void> handleQuit(UUID uniqueId);

    /**
     * Saves and disposes every active session.
     *
     * @return async completion
     */
    CompletableFuture<Void> shutdown();

    /**
     * Looks up an online session.
     *
     * @param uniqueId player uuid
     * @return session if present
     */
    Optional<PlayerSession> session(UUID uniqueId);

    /**
     * @return immutable snapshot of online sessions
     */
    Collection<PlayerSession> sessions();
}

