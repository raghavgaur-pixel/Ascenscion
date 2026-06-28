package com.ascension.equipment.runtime;

import com.ascension.equipment.event.EquipmentChangedEvent;
import com.ascension.equipment.event.ItemEquippedEvent;
import com.ascension.equipment.event.ItemUnequippedEvent;
import com.ascension.equipment.model.EquipmentSlot;
import com.ascension.equipment.model.EquipmentSnapshot;
import com.ascension.equipment.model.EquipmentTransaction;
import com.ascension.equipment.rule.EquipmentRule;
import com.ascension.equipment.rule.EquipmentRuleRegistry;
import com.ascension.events.EventBus;
import com.ascension.items.runtime.AscensionItem;
import com.ascension.session.model.PlayerSession;
import com.ascension.validation.ValidationCollector;
import com.ascension.validation.ValidationReport;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultEquipmentContainer implements EquipmentContainer {

    private final Map<EquipmentSlot, AscensionItem> items = new EnumMap<>(EquipmentSlot.class);
    private final PlayerSession session;
    private final EquipmentRuleRegistry ruleRegistry;
    private final EventBus eventBus;

    public DefaultEquipmentContainer(
        final PlayerSession session,
        final EquipmentRuleRegistry ruleRegistry,
        final EventBus eventBus
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    private DefaultEquipmentContainer(
        final PlayerSession session,
        final Map<EquipmentSlot, AscensionItem> pendingState
    ) {
        this.session = session;
        this.ruleRegistry = new EquipmentRuleRegistry();
        this.eventBus = null;
        this.items.putAll(pendingState);
    }

    @Override
    public Optional<AscensionItem> item(final EquipmentSlot slot) {
        return Optional.ofNullable(this.items.get(slot));
    }

    @Override
    public EquipmentSnapshot snapshot() {
        return new EquipmentSnapshot(this.items);
    }

    @Override
    public EquipmentTransaction startTransaction() {
        return new TransactionImpl();
    }

    private final class TransactionImpl implements EquipmentTransaction {

        private final Map<EquipmentSlot, AscensionItem> pending = new EnumMap<>(DefaultEquipmentContainer.this.items);

        @Override
        public EquipmentTransaction equip(final EquipmentSlot slot, final AscensionItem item) {
            this.pending.put(Objects.requireNonNull(slot, "slot"), Objects.requireNonNull(item, "item"));
            return this;
        }

        @Override
        public EquipmentTransaction unequip(final EquipmentSlot slot) {
            this.pending.remove(Objects.requireNonNull(slot, "slot"));
            return this;
        }

        @Override
        public ValidationReport commit() {
            final ValidationCollector collector = new ValidationCollector();
            final DefaultEquipmentContainer tempContainer = new DefaultEquipmentContainer(DefaultEquipmentContainer.this.session, this.pending);
            final EquipmentContext context = new EquipmentContext(DefaultEquipmentContainer.this.session, tempContainer);

            for (final EquipmentRule rule : DefaultEquipmentContainer.this.ruleRegistry.rules()) {
                collector.merge(rule.validate(context));
            }

            final ValidationReport report = collector.report();
            if (report.hasErrors()) {
                return report;
            }

            final Map<EquipmentSlot, AscensionItem> oldItems = new EnumMap<>(DefaultEquipmentContainer.this.items);
            DefaultEquipmentContainer.this.items.clear();
            DefaultEquipmentContainer.this.items.putAll(this.pending);

            if (DefaultEquipmentContainer.this.eventBus != null) {
                for (final EquipmentSlot slot : EquipmentSlot.values()) {
                    final AscensionItem oldItem = oldItems.get(slot);
                    final AscensionItem newItem = this.pending.get(slot);

                    if (oldItem != null && (newItem == null || !oldItem.instanceId().equals(newItem.instanceId()))) {
                        DefaultEquipmentContainer.this.eventBus.publish(new ItemUnequippedEvent(DefaultEquipmentContainer.this.session, slot, oldItem));
                    }
                    if (newItem != null && (oldItem == null || !newItem.instanceId().equals(oldItem.instanceId()))) {
                        DefaultEquipmentContainer.this.eventBus.publish(new ItemEquippedEvent(DefaultEquipmentContainer.this.session, slot, newItem));
                    }
                }
                DefaultEquipmentContainer.this.eventBus.publish(new EquipmentChangedEvent(DefaultEquipmentContainer.this.session));
            }

            return report;
        }

        @Override
        public void rollback() {
            this.pending.clear();
            this.pending.putAll(DefaultEquipmentContainer.this.items);
        }
    }
}
