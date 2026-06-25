package com.ascension.events.lifecycle;

import com.ascension.events.AscensionEvent;

/**
 * Published before a module is stopped.
 *
 * @param moduleId module identifier
 */
public record ModuleUnloadedEvent(String moduleId) implements AscensionEvent {
}

