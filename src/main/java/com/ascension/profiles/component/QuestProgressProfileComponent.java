package com.ascension.profiles.component;

import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent quest state for a character. */
public final class QuestProgressProfileComponent implements ProfileComponent {

    private final Map<String, Integer> progress;
    private final java.util.Set<String> completed;

    public QuestProgressProfileComponent() {
        this(Map.of(), java.util.Set.of());
    }

    public QuestProgressProfileComponent(final Map<String, Integer> progress, final java.util.Set<String> completed) {
        this.progress = new java.util.concurrent.ConcurrentHashMap<>(progress);
        this.completed = java.util.concurrent.ConcurrentHashMap.newKeySet();
        this.completed.addAll(completed);
    }

    public boolean isCompleted(final String questId) { return this.completed.contains(questId); }

    public void complete(final String questId) { this.completed.add(questId); }

    public int objectiveProgress(final String key) { return this.progress.getOrDefault(key, 0); }

    public int addProgress(final String key, final int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        return this.progress.merge(key, amount, Integer::sum);
    }

    public Map<String, Integer> progressSnapshot() { return Map.copyOf(this.progress); }

    public java.util.Set<String> completedSnapshot() { return java.util.Set.copyOf(this.completed); }

    public static final class Definition implements ProfileComponentDefinition<QuestProgressProfileComponent> {
        @Override public String id() { return "quests"; }
        @Override public Class<QuestProgressProfileComponent> type() { return QuestProgressProfileComponent.class; }
        @Override public QuestProgressProfileComponent createDefault(final ProfileLoadRequest request) { return new QuestProgressProfileComponent(); }
        @Override public SerializedObject serialize(final QuestProgressProfileComponent component) {
            return SerializedObject.builder()
                .put("progress", new LinkedHashMap<>(component.progressSnapshot()))
                .put("completed", component.completedSnapshot())
                .build();
        }
        @Override public QuestProgressProfileComponent deserialize(final SerializedObject data) {
            final Map<String, Integer> progress = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : data.getObject("progress").orElse(SerializedObject.empty()).asMap().entrySet()) {
                if (entry.getValue() instanceof Number number) progress.put(entry.getKey(), number.intValue());
            }
            return new QuestProgressProfileComponent(progress, data.getStringList("completed").stream().collect(java.util.stream.Collectors.toSet()));
        }
    }
}
