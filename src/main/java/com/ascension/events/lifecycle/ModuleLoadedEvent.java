package com.ascension.events.lifecycle;

import com.ascension.events.AscensionEvent;

/**
 * Published after a module starts successfully.
 *
 * @param moduleId module identifier
 */
public record ModuleLoadedEvent(String moduleId) implements AscensionEvent {
}

