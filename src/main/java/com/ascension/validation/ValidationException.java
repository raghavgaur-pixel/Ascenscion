package com.ascension.validation;

/**
 * Exception raised when validation fails in a fail-fast workflow.
 */
public final class ValidationException extends RuntimeException {

    private final ValidationReport report;

    public ValidationException(final String message, final ValidationReport report) {
        super(message);
        this.report = report;
    }

    /**
     * @return associated validation report
     */
    public ValidationReport report() {
        return this.report;
    }
}

