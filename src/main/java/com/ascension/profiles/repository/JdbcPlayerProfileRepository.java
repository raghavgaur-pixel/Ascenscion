package com.ascension.profiles.repository;

import com.ascension.database.dao.JdbcDaoSupport;
import com.ascension.database.service.DatabaseService;
import com.ascension.database.service.DatabaseTransaction;
import com.ascension.profiles.component.ProfileComponent;
import com.ascension.profiles.component.ProfileComponentContainer;
import com.ascension.profiles.component.ProfileComponentDefinition;
import com.ascension.profiles.model.PlayerProfile;
import com.ascension.profiles.model.ProfileLoadRequest;
import com.ascension.serialization.SerializedObject;
import com.ascension.serialization.YamlSerializedObjectCodec;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JDBC aggregate repository for base profile metadata and component payload data.
 */
public final class JdbcPlayerProfileRepository extends JdbcDaoSupport implements PlayerProfileRepository {

    private static final String UPSERT_PROFILE_SQL =
        """
        INSERT INTO player_profiles (
            player_id,
            username,
            display_name,
            first_join,
            last_join,
            playtime_seconds
        ) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (player_id) DO UPDATE SET
            username = EXCLUDED.username,
            display_name = EXCLUDED.display_name,
            first_join = EXCLUDED.first_join,
            last_join = EXCLUDED.last_join,
            playtime_seconds = EXCLUDED.playtime_seconds
        """;

    private final YamlSerializedObjectCodec codec;

    public JdbcPlayerProfileRepository(final DatabaseService databaseService, final YamlSerializedObjectCodec codec) {
        super(databaseService);
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public CompletableFuture<PlayerProfile> load(
        final ProfileLoadRequest request,
        final Collection<ProfileComponentDefinition<?>> componentDefinitions
    ) {
        return this.inTransaction(transaction -> this.loadProfile(transaction, request, componentDefinitions));
    }

    @Override
    public CompletableFuture<Void> save(
        final PlayerProfile profile,
        final Collection<ProfileComponentDefinition<?>> componentDefinitions
    ) {
        return this.inTransaction(transaction -> {
            this.saveProfile(transaction, profile, componentDefinitions);
            return null;
        });
    }

    private PlayerProfile loadProfile(
        final DatabaseTransaction transaction,
        final ProfileLoadRequest request,
        final Collection<ProfileComponentDefinition<?>> componentDefinitions
    ) throws SQLException {
        final java.util.Optional<StoredProfile> storedProfileResult = this.queryOne(
            transaction,
            """
            SELECT player_id, username, display_name, first_join, last_join, playtime_seconds
            FROM player_profiles
            WHERE player_id = ?
            """,
            statement -> statement.setString(1, request.uniqueId().toString()),
            resultSet -> new StoredProfile(
                UUID.fromString(resultSet.getString("player_id")),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                Instant.ofEpochMilli(resultSet.getLong("first_join")),
                Instant.ofEpochMilli(resultSet.getLong("last_join")),
                resultSet.getLong("playtime_seconds")
            )
        );
        final StoredProfile storedProfile = storedProfileResult.orElseGet(() -> new StoredProfile(
                request.uniqueId(),
                request.username(),
                request.displayName(),
                request.joinedAt(),
                request.joinedAt(),
                0L
            )
        );

        final Map<String, String> componentPayloads = new LinkedHashMap<>();
        for (final ComponentPayload payload : this.queryList(
            transaction,
            """
            SELECT component_id, payload
            FROM profile_component_data
            WHERE player_id = ?
            """,
            statement -> statement.setString(1, request.uniqueId().toString()),
            resultSet -> new ComponentPayload(
                resultSet.getString("component_id"),
                resultSet.getString("payload")
            )
        )) {
            componentPayloads.put(payload.componentId(), payload.payload());
        }

        final Map<String, ProfileComponent> components = new LinkedHashMap<>();
        for (final ProfileComponentDefinition<?> definition : componentDefinitions) {
            final ProfileComponent component = this.deserializeComponent(request, definition, componentPayloads);
            components.put(definition.id(), component);
        }

        final PlayerProfile profile = new PlayerProfile(
            storedProfile.uniqueId(),
            request.username(),
            request.displayName(),
            storedProfile.firstJoin(),
            request.joinedAt(),
            storedProfile.playtimeSeconds(),
            new ProfileComponentContainer(components)
        );
        if (storedProfileResult.isEmpty()) {
            this.saveProfile(transaction, profile, componentDefinitions);
        }
        return profile;
    }

    private void saveProfile(
        final DatabaseTransaction transaction,
        final PlayerProfile profile,
        final Collection<ProfileComponentDefinition<?>> componentDefinitions
    ) throws SQLException {
        this.update(
            transaction,
            UPSERT_PROFILE_SQL,
            statement -> {
                statement.setString(1, profile.uniqueId().toString());
                statement.setString(2, profile.username());
                statement.setString(3, profile.displayName());
                statement.setLong(4, profile.firstJoin().toEpochMilli());
                statement.setLong(5, profile.lastJoin().toEpochMilli());
                statement.setLong(6, profile.playtimeSeconds());
            }
        );

        this.update(
            transaction,
            "DELETE FROM profile_component_data WHERE player_id = ?",
            statement -> statement.setString(1, profile.uniqueId().toString())
        );

        for (final ProfileComponentDefinition<?> definition : componentDefinitions) {
            final String payload = this.codec.encode(this.serializeComponent(profile, definition));
            this.update(
                transaction,
                """
                INSERT INTO profile_component_data (player_id, component_id, payload, updated_at)
                VALUES (?, ?, ?, ?)
                """,
                statement -> {
                    statement.setString(1, profile.uniqueId().toString());
                    statement.setString(2, definition.id());
                    statement.setString(3, payload);
                    statement.setLong(4, System.currentTimeMillis());
                }
            );
        }
    }

    private ProfileComponent deserializeComponent(
        final ProfileLoadRequest request,
        final ProfileComponentDefinition<?> definition,
        final Map<String, String> payloads
    ) {
        final String payload = payloads.get(definition.id());
        if (payload == null) {
            return definition.createDefault(request);
        }
        return this.decode(definition, this.codec.decode(payload));
    }

    private SerializedObject serializeComponent(
        final PlayerProfile profile,
        final ProfileComponentDefinition<?> definition
    ) {
        return this.encode(definition, profile.components().require(castDefinition(definition)));
    }

    @SuppressWarnings("unchecked")
    private <T extends ProfileComponent> SerializedObject encode(
        final ProfileComponentDefinition<?> definition,
        final T component
    ) {
        final ProfileComponentDefinition<T> typedDefinition = (ProfileComponentDefinition<T>) definition;
        return typedDefinition.serialize(component);
    }

    @SuppressWarnings("unchecked")
    private <T extends ProfileComponent> T decode(
        final ProfileComponentDefinition<?> definition,
        final SerializedObject data
    ) {
        final ProfileComponentDefinition<T> typedDefinition = (ProfileComponentDefinition<T>) definition;
        return typedDefinition.deserialize(data);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ProfileComponent> ProfileComponentDefinition<T> castDefinition(
        final ProfileComponentDefinition<?> definition
    ) {
        return (ProfileComponentDefinition<T>) definition;
    }

    private record StoredProfile(
        UUID uniqueId,
        String username,
        String displayName,
        Instant firstJoin,
        Instant lastJoin,
        long playtimeSeconds
    ) {
    }

    private record ComponentPayload(String componentId, String payload) {
    }
}
