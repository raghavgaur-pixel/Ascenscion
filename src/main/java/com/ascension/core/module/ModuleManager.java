package com.ascension.core.module;

import com.ascension.core.service.ServiceRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModuleManager {

    private final Map<String, AscensionModule> modulesById;
    private final List<AscensionModule> startupOrder;

    public ModuleManager(final Collection<? extends AscensionModule> modules) {
        this.modulesById = indexModules(modules);
        this.startupOrder = topologicalSort(this.modulesById);
    }

    public void startAll(final ServiceRegistry services) {
        for (final AscensionModule module : this.startupOrder) {
            module.start(services);
        }
    }

    public void stopAll(final ServiceRegistry services) {
        final Deque<AscensionModule> reverseOrder = new ArrayDeque<>(this.startupOrder);
        while (!reverseOrder.isEmpty()) {
            reverseOrder.removeLast().stop(services);
        }
    }

    private static Map<String, AscensionModule> indexModules(final Collection<? extends AscensionModule> modules) {
        final Map<String, AscensionModule> indexed = new HashMap<>();
        for (final AscensionModule module : modules) {
            final AscensionModule previous = indexed.putIfAbsent(module.id(), module);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate module id detected: " + module.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private static List<AscensionModule> topologicalSort(final Map<String, AscensionModule> modulesById) {
        final List<AscensionModule> ordered = new ArrayList<>();
        final Set<String> visiting = new HashSet<>();
        final Set<String> visited = new HashSet<>();

        for (final String moduleId : modulesById.keySet()) {
            visit(moduleId, modulesById, visiting, visited, ordered);
        }

        return List.copyOf(ordered);
    }

    private static void visit(
        final String moduleId,
        final Map<String, AscensionModule> modulesById,
        final Set<String> visiting,
        final Set<String> visited,
        final List<AscensionModule> ordered
    ) {
        if (visited.contains(moduleId)) {
            return;
        }
        if (!visiting.add(moduleId)) {
            throw new IllegalStateException("Circular module dependency detected at: " + moduleId);
        }

        final AscensionModule module = modulesById.get(moduleId);
        if (module == null) {
            throw new IllegalStateException("Module not found: " + moduleId);
        }

        for (final String dependencyId : module.dependencies()) {
            if (!modulesById.containsKey(dependencyId)) {
                throw new IllegalStateException(
                    "Module '" + moduleId + "' depends on missing module '" + dependencyId + "'."
                );
            }
            visit(dependencyId, modulesById, visiting, visited, ordered);
        }

        visiting.remove(moduleId);
        visited.add(moduleId);
        ordered.add(module);
    }
}

