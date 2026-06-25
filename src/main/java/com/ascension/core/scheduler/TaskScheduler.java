package com.ascension.core.scheduler;

public interface TaskScheduler {

    void runSync(Runnable task);

    void runAsync(Runnable task);
}

