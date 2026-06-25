package com.ascension.assets.loader;

import com.ascension.assets.model.AssetDefinition;
import com.ascension.assets.model.AssetId;
import com.ascension.assets.model.AssetReference;
import com.ascension.assets.registry.AssetRegistry;
import com.ascension.assets.registry.RegistryBackedAssetRegistry;
import com.ascension.core.config.typed.TypedConfigurationService;
import com.ascension.core.logging.PluginLogger;
import com.ascension.core.platform.PluginPlatform;
import com.ascension.registry.AscensionRegistries;
import com.ascension.registry.MutableRegistry;
import com.ascension.registry.Registry;
import com.ascension.registry.RegistryHub;
import com.ascension.registry.ReloadableRegistry;
import com.ascension.serialization.SerializedFormat;
import com.ascension.serialization.SerializedObject;
import com.ascension.serialization.SerializedObjectCodec;
import com.ascension.serialization.SerializedObjectCodecRegistry;
import com.ascension.validation.DefaultValidationReport;
import com.ascension.validation.DependencyGraphValidator;
import com.ascension.validation.DuplicateDetector;
import com.ascension.validation.ValidationCollector;
import com.ascension.validation.ValidationException;
import com.ascension.validation.ValidationIssue;
import com.ascension.validation.ValidationReport;
import com.ascension.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Default reload-safe asset service with discovery, validation, inheritance, and registry integration.
 */
public final class DefaultAssetService implements AssetService {

    private final PluginPlatform platform;
    private final PluginLogger logger;
    private final RegistryHub registryHub;
    private final SerializedObjectCodecRegistry codecRegistry;
    private final TypedConfigurationService typedConfigurationService;
    private final Supplier<AssetFrameworkSettings> settingsSupplier;
    private final ConcurrentMap<String, AssetType<?>> assetTypes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AssetRegistry<?>> registries = new ConcurrentHashMap<>();
    private final Object reloadLock = new Object();

    public DefaultAssetService(
        final PluginPlatform platform,
        final PluginLogger logger,
        final RegistryHub registryHub,
        final SerializedObjectCodecRegistry codecRegistry,
        final TypedConfigurationService typedConfigurationService,
        final Supplier<AssetFrameworkSettings> settingsSupplier
    ) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.registryHub = Objects.requireNonNull(registryHub, "registryHub");
        this.codecRegistry = Objects.requireNonNull(codecRegistry, "codecRegistry");
        this.typedConfigurationService = Objects.requireNonNull(typedConfigurationService, "typedConfigurationService");
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
    }

    @Override
    public <T extends AssetDefinition> void registerType(final AssetType<T> assetType) {
        Objects.requireNonNull(assetType, "assetType");

        final AssetType<?> previous = this.assetTypes.putIfAbsent(assetType.id(), assetType);
        if (previous != null) {
            throw new IllegalStateException("Asset type already registered: " + assetType.id());
        }

        final ReloadableRegistry<AssetId, T> registry = this.registryHub.getOrCreateReloadable(assetType.registryDescriptor());
        this.registries.put(assetType.id(), new RegistryBackedAssetRegistry<>(registry));

        final MutableRegistry<String, AssetType<?>> assetTypeRegistry = this.registryHub.getOrCreate(AscensionRegistries.ASSET_TYPES);
        assetTypeRegistry.register(assetType.id(), castAssetType(assetType));
    }

    @Override
    public <T extends AssetDefinition> AssetRegistry<T> registry(final AssetType<T> assetType) {
        final AssetRegistry<?> registry = this.registries.get(Objects.requireNonNull(assetType, "assetType").id());
        if (registry == null) {
            throw new IllegalStateException("Asset registry is not registered: " + assetType.id());
        }
        return castAssetRegistry(registry);
    }

    @Override
    public AssetType<?> requireType(final String assetTypeId) {
        final AssetType<?> assetType = this.assetTypes.get(Objects.requireNonNull(assetTypeId, "assetTypeId"));
        if (assetType == null) {
            throw new IllegalStateException("Unknown asset type: " + assetTypeId);
        }
        return assetType;
    }

    @Override
    public Collection<AssetType<?>> assetTypes() {
        return List.copyOf(this.assetTypes.values());
    }

    @Override
    public AssetReloadResult reloadAllAssets() {
        synchronized (this.reloadLock) {
            final AssetFrameworkSettings settings = this.settingsSupplier.get();
            ensureDirectoryLayout(settings);

            final Map<String, LoadedAssetGroup> loadedGroups = new LinkedHashMap<>();
            final ValidationCollector collector = new ValidationCollector();
            for (final AssetType<?> assetType : orderedAssetTypes()) {
                final LoadedAssetGroup loadedGroup = loadGroup(assetType, settings);
                loadedGroups.put(assetType.id(), loadedGroup);
                collector.merge(loadedGroup.report());
            }

            collector.merge(validateGraph(snapshotDefinitions(loadedGroups), settings));
            final ValidationReport report = collector.report();
            logValidationReport("all-assets", report);
            ensureBlockingIssuesAbsent(settings, "all assets", report);

            commitAll(loadedGroups);
            return new AssetReloadResult(
                "all-assets",
                loadedGroups.values().stream().mapToInt(group -> group.values().size()).sum(),
                report
            );
        }
    }

    @Override
    public AssetReloadResult reloadAssetGroup(final String assetTypeId) {
        synchronized (this.reloadLock) {
            final AssetType<?> assetType = this.requireType(assetTypeId);
            final AssetFrameworkSettings settings = this.settingsSupplier.get();
            ensureDirectoryLayout(settings);

            final LoadedAssetGroup loadedGroup = loadGroup(assetType, settings);
            final Map<String, Map<AssetId, AssetDefinition>> definitions = snapshotCurrentDefinitions();
            definitions.put(assetType.id(), castDefinitionMap(loadedGroup.values()));

            final ValidationCollector collector = new ValidationCollector();
            collector.merge(loadedGroup.report());
            collector.merge(validateGraph(definitions, settings));

            final ValidationReport report = collector.report();
            logValidationReport(assetTypeId, report);
            ensureBlockingIssuesAbsent(settings, "asset group '" + assetTypeId + "'", report);

            commitUntyped(assetType, loadedGroup.values());
            return new AssetReloadResult(assetTypeId, loadedGroup.values().size(), report);
        }
    }

    @Override
    public void reloadOwnedConfiguration(final String owner) {
        this.typedConfigurationService.reloadOwner(owner);
    }

    private List<AssetType<?>> orderedAssetTypes() {
        return this.assetTypes.values().stream()
            .sorted(Comparator.comparing(AssetType::id))
            .toList();
    }

    private LoadedAssetGroup loadGroup(final AssetType<?> assetType, final AssetFrameworkSettings settings) {
        final ValidationCollector collector = new ValidationCollector();
        final Path directory = resolveGroupDirectory(settings, assetType);
        final List<RawAssetDocument> rawDocuments = discoverDocuments(directory, collector);
        final ValidationReport duplicateReport = new DuplicateDetector<RawAssetDocument, AssetId>(RawAssetDocument::id)
            .validate(rawDocuments, "assets." + assetType.id());
        collector.merge(duplicateReport);

        final Map<AssetId, RawAssetDocument> indexed = new LinkedHashMap<>();
        for (final RawAssetDocument rawDocument : rawDocuments) {
            indexed.putIfAbsent(rawDocument.id(), rawDocument);
        }

        final Map<AssetId, SerializedObject> resolvedDocuments = new LinkedHashMap<>();
        final Set<AssetId> inheritanceStack = new LinkedHashSet<>();
        for (final RawAssetDocument rawDocument : rawDocuments) {
            resolveInheritance(assetType, rawDocument.id(), indexed, resolvedDocuments, inheritanceStack, collector);
        }

        final Map<AssetId, AssetDefinition> values = new LinkedHashMap<>();
        for (final RawAssetDocument rawDocument : rawDocuments) {
            final SerializedObject resolved = resolvedDocuments.get(rawDocument.id());
            if (resolved == null) {
                continue;
            }
            try {
                final AssetDefinition definition = deserialize(assetType, rawDocument.source(), resolved);
                collector.merge(validateDefinition(assetType, definition, settings));
                values.put(definition.id(), definition);
            } catch (final Exception exception) {
                collector.error(
                    "asset_deserialize_failed",
                    rawDocument.source().path().toString(),
                    "Failed to deserialize asset '" + rawDocument.id() + "': " + exception.getMessage()
                );
            }
        }

        return new LoadedAssetGroup(assetType, values, collector.report());
    }

    private List<RawAssetDocument> discoverDocuments(final Path directory, final ValidationCollector collector) {
        final List<RawAssetDocument> documents = new ArrayList<>();
        if (!Files.exists(directory)) {
            return documents;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                .sorted()
                .forEach(path -> parseDocument(path, collector).ifPresent(documents::add));
        } catch (final IOException exception) {
            collector.error(
                "asset_discovery_failed",
                directory.toString(),
                "Failed to discover assets in " + directory + ": " + exception.getMessage()
            );
        }
        return documents;
    }

    private Optional<RawAssetDocument> parseDocument(final Path path, final ValidationCollector collector) {
        final SerializedFormat format = detectFormat(path).orElse(null);
        if (format == null) {
            return Optional.empty();
        }

        try {
            final SerializedObjectCodec codec = this.codecRegistry.require(format);
            final String payload = Files.readString(path, StandardCharsets.UTF_8);
            final SerializedObject object = codec.decode(payload);
            final String rawId = object.getString("id", "").trim();
            if (rawId.isEmpty()) {
                collector.error("asset_missing_id", path.toString(), "Asset file is missing required id.");
                return Optional.empty();
            }
            return Optional.of(new RawAssetDocument(AssetId.parse(rawId), new AssetSource(path, format), object));
        } catch (final Exception exception) {
            collector.error(
                "asset_parse_failed",
                path.toString(),
                "Failed to parse asset file: " + exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private SerializedObject resolveInheritance(
        final AssetType<?> assetType,
        final AssetId assetId,
        final Map<AssetId, RawAssetDocument> indexed,
        final Map<AssetId, SerializedObject> resolvedDocuments,
        final Set<AssetId> inheritanceStack,
        final ValidationCollector collector
    ) {
        final SerializedObject cached = resolvedDocuments.get(assetId);
        if (cached != null) {
            return cached;
        }

        if (!inheritanceStack.add(assetId)) {
            collector.error(
                "asset_inheritance_cycle",
                "assets." + assetType.id() + "." + assetId,
                "Cyclic asset inheritance detected for " + assetId
            );
            return null;
        }

        final RawAssetDocument rawDocument = indexed.get(assetId);
        if (rawDocument == null) {
            inheritanceStack.remove(assetId);
            return null;
        }

        SerializedObject resolved = rawDocument.payload().without("extends");
        final String parentId = rawDocument.payload().getString("extends", "").trim();
        if (!parentId.isEmpty()) {
            try {
                final AssetId inheritedId = AssetId.parse(parentId);
                final RawAssetDocument parent = indexed.get(inheritedId);
                if (parent == null) {
                    collector.error(
                        "asset_missing_parent",
                        rawDocument.source().path().toString(),
                        "Asset parent not found: " + inheritedId
                    );
                } else {
                    final SerializedObject parentDocument = resolveInheritance(
                        assetType,
                        inheritedId,
                        indexed,
                        resolvedDocuments,
                        inheritanceStack,
                        collector
                    );
                    if (parentDocument != null) {
                        resolved = parentDocument.merge(resolved);
                    }
                }
            } catch (final Exception exception) {
                collector.error(
                    "asset_invalid_parent",
                    rawDocument.source().path().toString(),
                    "Invalid parent asset id: " + parentId
                );
            }
        }

        inheritanceStack.remove(assetId);
        resolvedDocuments.put(assetId, resolved);
        return resolved;
    }

    private ValidationReport validateDefinition(
        final AssetType<?> assetType,
        final AssetDefinition definition,
        final AssetFrameworkSettings settings
    ) {
        final ValidationCollector collector = new ValidationCollector();
        if (definition.id().namespace().isBlank()) {
            collector.error("asset_namespace_blank", definition.id().toString(), "Asset namespace must not be blank.");
        }
        if (definition.id().value().isBlank()) {
            collector.error("asset_value_blank", definition.id().toString(), "Asset value must not be blank.");
        }
        if (definition.displayName().isBlank()) {
            collector.error("asset_display_name_blank", definition.id().toString(), "Asset display name must not be blank.");
        }
        if (definition.description().isBlank()) {
            collector.error("asset_description_blank", definition.id().toString(), "Asset description must not be blank.");
        }
        if (!definition.compatibility().supports(settings.engineVersion())) {
            collector.error(
                "asset_incompatible_version",
                definition.id().toString(),
                "Asset is incompatible with engine version " + settings.engineVersion()
            );
        }
        collector.merge(validateTyped(assetType.validator(), definition));
        return collector.report();
    }

    private ValidationReport validateGraph(
        final Map<String, Map<AssetId, AssetDefinition>> definitionsByType,
        final AssetFrameworkSettings settings
    ) {
        final ValidationCollector collector = new ValidationCollector();
        final List<DependencyNode> nodes = new ArrayList<>();

        for (final Map.Entry<String, Map<AssetId, AssetDefinition>> groupEntry : definitionsByType.entrySet()) {
            for (final AssetDefinition definition : groupEntry.getValue().values()) {
                for (final AssetReference dependency : definition.dependencies()) {
                    final Map<AssetId, AssetDefinition> dependencyGroup = definitionsByType.get(dependency.type());
                    if (dependencyGroup == null) {
                        collector.error(
                            "asset_unknown_dependency_type",
                            definition.id().toString(),
                            "Unknown dependency type '" + dependency.type() + "' referenced by " + definition.id()
                        );
                        continue;
                    }
                    if (!dependencyGroup.containsKey(dependency.id())) {
                        collector.error(
                            "asset_missing_dependency",
                            definition.id().toString(),
                            "Missing dependency '" + dependency.type() + ":" + dependency.id() + "'."
                        );
                    }
                }
                nodes.add(DependencyNode.from(definition));
            }
        }

        collector.merge(new DependencyGraphValidator<DependencyNode, String>(
            DependencyNode::key,
            DependencyNode::dependencies
        ).validate(nodes, "assets"));

        if (settings.strictValidation() && !collector.report().issues().isEmpty()) {
            return collector.report();
        }
        return collector.report();
    }

    private Map<String, Map<AssetId, AssetDefinition>> snapshotDefinitions(final Map<String, LoadedAssetGroup> groups) {
        final Map<String, Map<AssetId, AssetDefinition>> definitions = new LinkedHashMap<>();
        for (final Map.Entry<String, LoadedAssetGroup> entry : groups.entrySet()) {
            definitions.put(entry.getKey(), entry.getValue().values());
        }
        return definitions;
    }

    private Map<String, Map<AssetId, AssetDefinition>> snapshotCurrentDefinitions() {
        final Map<String, Map<AssetId, AssetDefinition>> definitions = new LinkedHashMap<>();
        for (final AssetType<?> assetType : orderedAssetTypes()) {
            final Registry<AssetId, ?> registry = this.registryHub.require(assetType.registryDescriptor());
            final Map<AssetId, AssetDefinition> values = new LinkedHashMap<>();
            for (final Object value : registry.values()) {
                values.put(((AssetDefinition) value).id(), (AssetDefinition) value);
            }
            definitions.put(assetType.id(), values);
        }
        return definitions;
    }

    private void commitAll(final Map<String, LoadedAssetGroup> groups) {
        for (final LoadedAssetGroup group : groups.values()) {
            commitUntyped(group.assetType(), group.values());
        }
    }

    private <T extends AssetDefinition> void commit(final AssetType<T> assetType, final Map<AssetId, T> values) {
        final ReloadableRegistry<AssetId, T> registry = this.registryHub.requireReloadable(assetType.registryDescriptor());
        registry.replaceAll(values);
    }

    @SuppressWarnings("unchecked")
    private void commitUntyped(final AssetType<?> assetType, final Map<AssetId, AssetDefinition> values) {
        this.commit((AssetType<AssetDefinition>) assetType, values);
    }

    private void ensureDirectoryLayout(final AssetFrameworkSettings settings) {
        final Path root = this.platform.dataFolder().toPath().resolve(settings.rootDirectory());
        createDirectories(root);
        for (final AssetType<?> assetType : this.assetTypes.values()) {
            createDirectories(resolveGroupDirectory(settings, assetType));
        }
    }

    private static void createDirectories(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create asset directory: " + path, exception);
        }
    }

    private Path resolveGroupDirectory(final AssetFrameworkSettings settings, final AssetType<?> assetType) {
        final String groupDirectory = settings.ownedDirectories().getOrDefault(assetType.id(), assetType.directory());
        return this.platform.dataFolder().toPath()
            .resolve(settings.rootDirectory())
            .resolve(groupDirectory);
    }

    private static Optional<SerializedFormat> detectFormat(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return Optional.of(SerializedFormat.YAML);
        }
        if (fileName.endsWith(".json")) {
            return Optional.of(SerializedFormat.JSON);
        }
        if (fileName.endsWith(".bin")) {
            return Optional.of(SerializedFormat.BINARY);
        }
        return Optional.empty();
    }

    private void ensureBlockingIssuesAbsent(
        final AssetFrameworkSettings settings,
        final String scope,
        final ValidationReport report
    ) {
        final boolean hasWarnings = report.issues().stream().anyMatch(issue -> issue.severity() == com.ascension.validation.ValidationSeverity.WARNING);
        if (report.hasErrors() || (settings.strictValidation() && hasWarnings)) {
            throw new ValidationException("Asset reload failed for " + scope + ".", report);
        }
    }

    private void logValidationReport(final String scope, final ValidationReport report) {
        for (final ValidationIssue issue : report.issues()) {
            final String message = "[" + scope + "] " + issue.code() + " at " + issue.path() + ": " + issue.message();
            switch (issue.severity()) {
                case INFO -> this.logger.info(message);
                case WARNING -> this.logger.warn(message);
                case ERROR -> this.logger.error(message);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends AssetDefinition> T deserialize(
        final AssetType<?> assetType,
        final AssetSource source,
        final SerializedObject object
    ) {
        return (T) ((AssetType<T>) assetType).serializer().deserialize(source, object, assetType.owner());
    }

    @SuppressWarnings("unchecked")
    private static ValidationReport validateTyped(final Validator<?> validator, final AssetDefinition definition) {
        return ((Validator<AssetDefinition>) validator).validate(definition);
    }

    @SuppressWarnings("unchecked")
    private static AssetType<?> castAssetType(final AssetType<?> assetType) {
        return (AssetType<?>) assetType;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AssetDefinition> AssetRegistry<T> castAssetRegistry(final AssetRegistry<?> registry) {
        return (AssetRegistry<T>) registry;
    }

    private record RawAssetDocument(
        AssetId id,
        AssetSource source,
        SerializedObject payload
    ) {
    }

    private record LoadedAssetGroup(
        AssetType<?> assetType,
        Map<AssetId, AssetDefinition> values,
        ValidationReport report
    ) {

        private LoadedAssetGroup {
            values = Map.copyOf(values);
            report = report == null ? new DefaultValidationReport(List.of()) : report;
        }
    }

    private record DependencyNode(String key, Collection<String> dependencies) {

        private static DependencyNode from(final AssetDefinition definition) {
            return new DependencyNode(
                key(definition.type(), definition.id()),
                definition.dependencies().stream()
                    .map(reference -> key(reference.type(), reference.id()))
                    .toList()
            );
        }

        private static String key(final String type, final AssetId id) {
            return type + "@" + id;
        }
    }
}
