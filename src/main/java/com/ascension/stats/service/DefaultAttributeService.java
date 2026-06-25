package com.ascension.stats.service;

import com.ascension.stats.attribute.AttributeContainer;
import java.util.Objects;

/**
 * Default attribute container factory service.
 */
public final class DefaultAttributeService implements AttributeService {

    private final StatService statService;
    private final DefaultAttributeCalculationService calculationService;

    public DefaultAttributeService(
        final StatService statService,
        final DefaultAttributeCalculationService calculationService
    ) {
        this.statService = Objects.requireNonNull(statService, "statService");
        this.calculationService = Objects.requireNonNull(calculationService, "calculationService");
    }

    @Override
    public AttributeContainer createContainer() {
        return new DefaultAttributeContainer(this.statService, this.calculationService);
    }

    @Override
    public StatService statService() {
        return this.statService;
    }
}
