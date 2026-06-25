package com.ascension.core.module;

import com.ascension.core.service.ServiceRegistry;
import java.util.Set;

public interface AscensionModule {

    String id();

    default Set<String> dependencies() {
        return Set.of();
    }

    void start(ServiceRegistry services);

    void stop(ServiceRegistry services);
}

