package com.ascension.stats.service;

import com.ascension.stats.attribute.AttributeContainer;

/**
 * Factory and facade for session-owned attribute containers.
 */
public interface AttributeService {

    /**
     * Creates a new runtime attribute container.
     *
     * @return fresh attribute container
     */
    AttributeContainer createContainer();

    /**
     * @return stat lookup service
     */
    StatService statService();
}
