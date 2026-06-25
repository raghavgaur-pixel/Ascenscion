package com.ascension.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable collector for validation findings.
 */
public final class ValidationCollector {

    private final List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Adds an info-level issue.
     *
     * @param code stable issue code
     * @param path logical path
     * @param message issue message
     */
    public void info(final String code, final String path, final String message) {
        this.add(ValidationSeverity.INFO, code, path, message);
    }

    /**
     * Adds a warning-level issue.
     *
     * @param code stable issue code
     * @param path logical path
     * @param message issue message
     */
    public void warning(final String code, final String path, final String message) {
        this.add(ValidationSeverity.WARNING, code, path, message);
    }

    /**
     * Adds an error-level issue.
     *
     * @param code stable issue code
     * @param path logical path
     * @param message issue message
     */
    public void error(final String code, final String path, final String message) {
        this.add(ValidationSeverity.ERROR, code, path, message);
    }

    /**
     * Merges another report into this collector.
     *
     * @param report report to merge
     */
    public void merge(final ValidationReport report) {
        this.issues.addAll(report.issues());
    }

    /**
     * @return immutable report
     */
    public ValidationReport report() {
        return new DefaultValidationReport(this.issues);
    }

    private void add(
        final ValidationSeverity severity,
        final String code,
        final String path,
        final String message
    ) {
        this.issues.add(new ValidationIssue(severity, code, path, message));
    }
}

