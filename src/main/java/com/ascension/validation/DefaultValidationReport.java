package com.ascension.validation;

import java.util.Collection;
import java.util.List;

/**
 * Default immutable validation report implementation.
 */
public record DefaultValidationReport(List<ValidationIssue> issues) implements ValidationReport {

    public DefaultValidationReport {
        issues = List.copyOf(issues);
    }


    @Override
    public boolean hasErrors() {
        return this.issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }
}

