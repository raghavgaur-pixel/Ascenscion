package com.ascension.validation;

/**
 * Generic validator contract.
 *
 * @param <T> validated type
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validates a value.
     *
     * @param value value to validate
     * @return validation report
     */
    ValidationReport validate(T value);

    /**
     * @return validator that always succeeds
     * @param <T> validated type
     */
    static <T> Validator<T> noop() {
        return value -> new DefaultValidationReport(java.util.List.of());
    }
}

