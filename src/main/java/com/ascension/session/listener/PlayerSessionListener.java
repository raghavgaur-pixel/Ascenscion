package com.ascension.session.listener;

import com.ascension.session.service.PlayerSessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bridges Bukkit join and quit events into the internal session lifecycle.
 */
public final class PlayerSessionListener implements Listener {

    private final PlayerSessionManager sessionManager;

    public PlayerSessionListener(final PlayerSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        this.sessionManager.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.sessionManager.handleQuit(event.getPlayer().getUniqueId());
    }
}

