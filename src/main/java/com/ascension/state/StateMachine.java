package com.ascension.state;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Thread-safe generic state machine with explicit transition validation.
 *
 * @param <S> state type
 */
public final class StateMachine<S> {

    private final BiPredicate<S, S> transitionRule;
    private volatile S state;

    public StateMachine(final S initialState, final BiPredicate<S, S> transitionRule) {
        this.state = Objects.requireNonNull(initialState, "initialState");
        this.transitionRule = Objects.requireNonNull(transitionRule, "transitionRule");
    }

    /**
     * @return current state
     */
    public S current() {
        return this.state;
    }

    /**
     * Attempts a state transition.
     *
     * @param nextState target state
     * @return {@code true} if transition succeeded
     */
    public synchronized boolean transitionTo(final S nextState) {
        Objects.requireNonNull(nextState, "nextState");
        if (!this.transitionRule.test(this.state, nextState)) {
            return false;
        }

        this.state = nextState;
        return true;
    }
}

