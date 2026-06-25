package com.ascension.validation;

import java.util.Objects;

/**
 * Single validation finding.
 *
 * @param severity issue severity
 * @param code stable issue code
 * @param path logical validation path
 * @param message human-readable message
 */
public record ValidationIssue(
    ValidationSeverity severity,
    String code,
    String path,
    String message
) {

    public ValidationIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(message, "message");
    }
}

