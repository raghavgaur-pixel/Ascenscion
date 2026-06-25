package com.ascension.state;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe runtime flag store.
 */
public final class RuntimeFlagContainer {

    private final Set<String> flags = ConcurrentHashMap.newKeySet();

    /**
     * Adds a flag.
     *
     * @param flag flag identifier
     * @return {@code true} if newly added
     */
    public boolean add(final String flag) {
        return this.flags.add(flag);
    }

    /**
     * Removes a flag.
     *
     * @param flag flag identifier
     * @return {@code true} if removed
     */
    public boolean remove(final String flag) {
        return this.flags.remove(flag);
    }

    /**
     * Checks for a flag.
     *
     * @param flag flag identifier
     * @return {@code true} if present
     */
    public boolean has(final String flag) {
        return this.flags.contains(flag);
    }

    /**
     * @return immutable snapshot of flags
     */
    public Set<String> snapshot() {
        return Set.copyOf(this.flags);
    }

    /**
     * Clears every flag.
     */
    public void clear() {
        this.flags.clear();
    }
}
