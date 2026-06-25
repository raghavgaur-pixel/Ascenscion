package com.ascension.session.model;

/**
 * High-level session lifecycle state.
 */
public enum PlayerSessionState {
    CREATED,
    LOADING_PROFILE,
    READY,
    LEAVING,
    DESTROYED
}

