package com.ascension.core.config.typed;

import com.ascension.core.logging.PluginLogger;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.serialization.SerializedFormat;
import com.ascension.serialization.SerializedObject;
import com.ascension.serialization.SerializedObjectCodec;
import com.ascension.serialization.SerializedObjectCodecRegistry;
import com.ascension.validation.ValidationException;
import com.ascension.validation.Validator;
import com.ascension.validation.ValidationReport;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default reload-safe typed configuration service backed by structured codecs.
 */
public final class DefaultTypedConfigurationService implements TypedConfigurationService {

    private final PluginPlatform platform;
    private final PluginLogger logger;
    private final SerializedObjectCodecRegistry codecRegistry;
    private final Map<String, ManagedConfiguration<?>> loaded = new ConcurrentHashMap<>();

    public DefaultTypedConfigurationService(
        final PluginPlatform platform,
        final PluginLogger logger,
        final SerializedObjectCodecRegistry codecRegistry
    ) {
        this.platform = platform;
        this.logger = logger;
        this.codecRegistry = codecRegistry;
    }

    @Override
    public <T> ManagedConfiguration<T> registerAndLoad(final TypedConfigurationDescriptor<T> descriptor) {
        final ManagedConfiguration<T> configuration = this.loadInternal(descriptor);
        this.loaded.put(descriptor.id(), configuration);
        return configuration;
    }

    @Override
    public <T> ManagedConfiguration<T> reload(final TypedConfigurationDescriptor<T> descriptor) {
        final ManagedConfiguration<T> configuration = this.loadInternal(descriptor);
        this.loaded.put(descriptor.id(), configuration);
        return configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ManagedConfiguration<T> require(final TypedConfigurationDescriptor<T> descriptor) {
        final ManagedConfiguration<?> configuration = this.loaded.get(descriptor.id());
        if (configuration == null) {
            throw new IllegalStateException("Configuration not loaded: " + descriptor.id());
        }
        return (ManagedConfiguration<T>) configuration;
    }

    @Override
    public void reloadOwner(final String owner) {
        for (final ManagedConfiguration<?> configuration : this.loaded.values()) {
            if (configuration.descriptor().owner().equals(owner)) {
                this.reloadUntyped(configuration.descriptor());
            }
        }
    }

    @Override
    public Collection<ManagedConfiguration<?>> loaded() {
        return java.util.List.copyOf(this.loaded.values());
    }

    private <T> ManagedConfiguration<T> loadInternal(final TypedConfigurationDescriptor<T> descriptor) {
        try {
            final File file = new File(this.platform.dataFolder(), descriptor.path());
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Failed to create config directory: " + parent.getAbsolutePath());
            }

            final SerializedObjectCodec codec = this.codecRegistry.require(descriptor.format());
            final SerializedObject rawObject;
            if (!file.exists()) {
                final T defaultValue = descriptor.defaultSupplier().get();
                validate(descriptor.id(), descriptor.validator(), defaultValue);
                rawObject = descriptor.codec().encode(defaultValue);
                write(descriptor, file, codec, rawObject);
                return new ManagedConfiguration<>(descriptor, descriptor.version(), defaultValue);
            }

            rawObject = codec.decode(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            final int storedVersion = (int) rawObject.getLong("version", descriptor.version());
            SerializedObject payload = extractPayload(rawObject);
            if (storedVersion < descriptor.version()) {
                payload = descriptor.upgrader().upgrade(storedVersion, payload, descriptor.version());
                write(descriptor, file, codec, payload);
            }

            final T value = descriptor.codec().decode(payload);
            validate(descriptor.id(), descriptor.validator(), value);

            return new ManagedConfiguration<>(descriptor, descriptor.version(), value);
        } catch (final ValidationException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to load configuration: " + descriptor.id(), exception);
        }
    }

    private void reloadUntyped(final TypedConfigurationDescriptor<?> descriptor) {
        this.logger.info("Reloading configuration '" + descriptor.id() + "'.");
        this.loaded.put(descriptor.id(), this.loadInternalUntyped(descriptor));
    }

    @SuppressWarnings("unchecked")
    private ManagedConfiguration<?> loadInternalUntyped(final TypedConfigurationDescriptor<?> descriptor) {
        return this.loadInternal((TypedConfigurationDescriptor<Object>) descriptor);
    }

    private static SerializedObject extractPayload(final SerializedObject rawObject) {
        final Object payload = rawObject.asMap().get("data");
        if (payload instanceof Map<?, ?> payloadMap) {
            final java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : payloadMap.entrySet()) {
                if (entry.getKey() != null) {
                    values.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return SerializedObject.copyOf(values);
        } else if (payload instanceof org.bukkit.configuration.ConfigurationSection section) {
            final java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : section.getValues(false).entrySet()) {
                if (entry.getKey() != null) {
                    values.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return SerializedObject.copyOf(values);
        }
        return rawObject;
    }

    private static <T> void write(
        final TypedConfigurationDescriptor<T> descriptor,
        final File file,
        final SerializedObjectCodec codec,
        final SerializedObject payload
    ) throws Exception {
        final SerializedObject document = SerializedObject.builder()
            .put("version", descriptor.version())
            .put("data", payload.asMap())
            .build();
        final StringBuilder builder = new StringBuilder();
        if (descriptor.format() == SerializedFormat.YAML) {
            for (final String header : descriptor.headerComments()) {
                builder.append("# ").append(header).append(System.lineSeparator());
            }
        }
        builder.append(codec.encode(document));
        Files.writeString(file.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    private static <T> void validate(
        final String configurationId,
        final Validator<T> validator,
        final T value
    ) {
        final ValidationReport report = validator.validate(value);
        if (report.hasErrors()) {
            throw new ValidationException("Configuration validation failed for " + configurationId, report);
        }
    }
}
