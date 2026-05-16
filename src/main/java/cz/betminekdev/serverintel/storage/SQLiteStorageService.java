package cz.betminekdev.serverintel.storage;

import cz.betminekdev.serverintel.timeline.TimelineEvent;
import cz.betminekdev.serverintel.timeline.TimelineEventType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SQLiteStorageService implements StorageService {
    private final File databaseFile;
    private Connection connection;

    public SQLiteStorageService(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public synchronized void initialize() throws SQLException {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Could not create database directory: " + parent.getAbsolutePath());
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA journal_mode=WAL");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "first_seen INTEGER NOT NULL,"
                    + "last_seen INTEGER NOT NULL,"
                    + "risk_score INTEGER NOT NULL DEFAULT 0,"
                    + "alerts_enabled INTEGER NOT NULL DEFAULT 1"
                    + ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS timeline_events ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_uuid TEXT NOT NULL,"
                    + "player_name TEXT NOT NULL,"
                    + "timestamp INTEGER NOT NULL,"
                    + "event_type TEXT NOT NULL,"
                    + "world TEXT,"
                    + "x INTEGER,"
                    + "y INTEGER,"
                    + "z INTEGER,"
                    + "risk_change INTEGER NOT NULL DEFAULT 0,"
                    + "reason TEXT NOT NULL,"
                    + "details TEXT NOT NULL DEFAULT ''"
                    + ")");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timeline_player_time ON timeline_events(player_uuid, timestamp DESC)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_timeline_type_time ON timeline_events(event_type, timestamp DESC)");
        }
    }

    @Override
    public synchronized void upsertPlayer(UUID uuid, String name, long timestamp) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players(uuid, name, first_seen, last_seen, risk_score, alerts_enabled) VALUES(?, ?, ?, ?, 0, 1) "
                        + "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, last_seen = excluded.last_seen")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3, timestamp);
            statement.setLong(4, timestamp);
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized Optional<PlayerProfile> findProfile(UUID uuid) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readProfile(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public synchronized Optional<PlayerProfile> findProfileByName(String name) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE lower(name) = lower(?) ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readProfile(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public synchronized void updateRisk(UUID uuid, int riskScore, long lastSeen) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET risk_score = ?, last_seen = ? WHERE uuid = ?")) {
            statement.setInt(1, riskScore);
            statement.setLong(2, lastSeen);
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void setAlertsEnabled(UUID uuid, boolean enabled) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET alerts_enabled = ? WHERE uuid = ?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized TimelineEvent addTimelineEvent(TimelineEvent event) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO timeline_events(player_uuid, player_name, timestamp, event_type, world, x, y, z, risk_change, reason, details) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, event.playerUuid().toString());
            statement.setString(2, event.playerName());
            statement.setLong(3, event.timestamp());
            statement.setString(4, event.eventType().name());
            statement.setString(5, event.world());
            setNullableInt(statement, 6, event.x());
            setNullableInt(statement, 7, event.y());
            setNullableInt(statement, 8, event.z());
            statement.setInt(9, event.riskChange());
            statement.setString(10, event.reason());
            statement.setString(11, event.details() == null ? "" : event.details());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return event.withId(keys.getLong(1));
                }
            }
        }
        return event;
    }

    @Override
    public synchronized List<TimelineEvent> getRecentTimeline(UUID uuid, int limit) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM timeline_events WHERE player_uuid = ? ORDER BY timestamp DESC, id DESC LIMIT ?")) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(1, limit));
            return readEvents(statement);
        }
    }

    @Override
    public synchronized List<TimelineEvent> getRecentRiskSignals(UUID uuid, int limit) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM timeline_events WHERE player_uuid = ? AND risk_change > 0 ORDER BY timestamp DESC, id DESC LIMIT ?")) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(1, limit));
            return readEvents(statement);
        }
    }

    @Override
    public synchronized int countTimelineEvents(UUID uuid, String eventType, String materialName, long sinceMillis) throws SQLException {
        ensureOpen();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM timeline_events WHERE player_uuid = ? AND event_type = ? AND timestamp >= ? AND details LIKE ?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, eventType);
            statement.setLong(3, sinceMillis);
            statement.setString(4, "%material=" + materialName + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public synchronized int decayRiskScores(int amount) throws SQLException {
        ensureOpen();
        if (amount <= 0) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE players SET risk_score = max(0, risk_score - ?) WHERE risk_score > 0")) {
            statement.setInt(1, amount);
            return statement.executeUpdate();
        }
    }

    @Override
    public synchronized void purgeOldTimelineEvents(int keepDataDays) throws SQLException {
        ensureOpen();
        long cutoff = System.currentTimeMillis() - (Math.max(1L, keepDataDays) * 86_400_000L);
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM timeline_events WHERE timestamp < ?")) {
            statement.setLong(1, cutoff);
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Shutdown should not throw into Bukkit.
        } finally {
            connection = null;
        }
    }

    private void ensureOpen() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("SQLite storage is not initialized.");
        }
    }

    private PlayerProfile readProfile(ResultSet resultSet) throws SQLException {
        return new PlayerProfile(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getString("name"),
                resultSet.getLong("first_seen"),
                resultSet.getLong("last_seen"),
                resultSet.getInt("risk_score"),
                resultSet.getInt("alerts_enabled") != 0
        );
    }

    private List<TimelineEvent> readEvents(PreparedStatement statement) throws SQLException {
        ArrayList<TimelineEvent> events = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                events.add(new TimelineEvent(
                        resultSet.getLong("id"),
                        UUID.fromString(resultSet.getString("player_uuid")),
                        resultSet.getString("player_name"),
                        resultSet.getLong("timestamp"),
                        TimelineEventType.valueOf(resultSet.getString("event_type")),
                        resultSet.getString("world"),
                        nullableInt(resultSet, "x"),
                        nullableInt(resultSet, "y"),
                        nullableInt(resultSet, "z"),
                        resultSet.getInt("risk_change"),
                        resultSet.getString("reason"),
                        resultSet.getString("details")
                ));
            }
        }
        return events;
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private static Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
