package com.ascension.stats.module;

import com.ascension.validation.ValidationCollector;
import com.ascension.validation.Validator;
import com.ascension.stats.definition.DerivedFormulaDefinition;
import com.ascension.stats.definition.StatCategory;
import com.ascension.stats.definition.StatDefinition;

/**
 * Shared validators for stat definitions.
 */
public final class StatValidators {

    public static final Validator<StatDefinition> STAT_DEFINITION = definition -> {
        final ValidationCollector collector = new ValidationCollector();
        if (definition.displayName().isBlank()) {
            collector.error("display_name_blank", definition.id().toString(), "Stat display name must not be blank.");
        }
        if (definition.decimalPlaces() > 6) {
            collector.warning("high_precision", definition.id().toString(), "Stat precision above 6 decimals may be wasteful.");
        }
        if (definition.minimumValue().isPresent() && definition.maximumValue().isPresent()
            && definition.minimumValue().orElseThrow() > definition.maximumValue().orElseThrow()) {
            collector.error("invalid_clamp", definition.id().toString(), "Minimum value cannot exceed maximum value.");
        }
        if (definition.category() == StatCategory.DERIVED && definition.formula().isEmpty()) {
            collector.error("missing_formula", definition.id().toString(), "Derived stats must declare a formula.");
        }
        if (definition.category() != StatCategory.DERIVED && definition.formula().isPresent()) {
            collector.warning("formula_on_non_derived", definition.id().toString(), "Non-derived stat declares a formula.");
        }
        definition.formula().ifPresent(formula -> validateFormula(definition, formula, collector));
        return collector.report();
    };

    private StatValidators() {
    }

    private static void validateFormula(
        final StatDefinition definition,
        final DerivedFormulaDefinition formula,
        final ValidationCollector collector
    ) {
        if (formula.type().isBlank()) {
            collector.error("formula_type_blank", definition.id().toString(), "Derived formula type must not be blank.");
        }
        if (formula.coefficients().isEmpty()) {
            collector.warning("formula_coefficients_empty", definition.id().toString(), "Derived formula has no dependencies.");
        }
        if (formula.coefficients().containsKey(definition.id())) {
            collector.error("formula_self_reference", definition.id().toString(), "Derived formula cannot depend on itself.");
        }
    }
}
