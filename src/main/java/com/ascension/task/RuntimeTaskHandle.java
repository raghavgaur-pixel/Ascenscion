package com.ascension.task;

/**
 * Handle to a scheduled runtime task.
 */
public interface RuntimeTaskHandle {

    /**
     * @return task owner identifier
     */
    String owner();

    /**
     * @return task name
     */
    String name();

    /**
     * @return execution mode
     */
    TaskExecutionMode mode();

    /**
     * Cancels the task.
     */
    void cancel();

    /**
     * @return {@code true} if cancelled
     */
    boolean cancelled();
}

