package com.ascension.validation;

import java.util.Collection;

/**
 * Immutable validation report.
 */
public interface ValidationReport {

    /**
     * @return immutable issues
     */
    Collection<ValidationIssue> issues();

    /**
     * @return {@code true} if any error-level issue exists
     */
    boolean hasErrors();
}

