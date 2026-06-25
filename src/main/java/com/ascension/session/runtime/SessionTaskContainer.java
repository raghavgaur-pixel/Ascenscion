package com.ascension.session.runtime;

import com.ascension.task.RuntimeTaskHandle;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks session-owned runtime tasks for safe disposal.
 */
public final class SessionTaskContainer {

    private final Map<String, RuntimeTaskHandle> tasks = new ConcurrentHashMap<>();

    /**
     * Registers a task handle.
     *
     * @param handle task handle
     */
    public void track(final RuntimeTaskHandle handle) {
        this.tasks.put(handle.name(), handle);
    }

    /**
     * Cancels and removes every tracked task.
     */
    public void cancelAll() {
        for (final RuntimeTaskHandle handle : this.tasks.values()) {
            handle.cancel();
        }
        this.tasks.clear();
    }

    /**
     * @return immutable snapshot
     */
    public Collection<RuntimeTaskHandle> snapshot() {
        return java.util.List.copyOf(this.tasks.values());
    }
}

