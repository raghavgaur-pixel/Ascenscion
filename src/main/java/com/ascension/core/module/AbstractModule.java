package com.ascension.core.module;

import com.ascension.core.service.ServiceRegistry;

public abstract class AbstractModule implements AscensionModule {

    @Override
    public final void start(final ServiceRegistry services) {
        this.onStart(services);
    }

    @Override
    public final void stop(final ServiceRegistry services) {
        this.onStop(services);
    }

    protected abstract void onStart(ServiceRegistry services);

    protected abstract void onStop(ServiceRegistry services);
}

