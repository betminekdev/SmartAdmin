package cz.betminekdev.smartadmin.storage;

import cz.betminekdev.smartadmin.timeline.TimelineEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StorageService extends AutoCloseable {
    void initialize() throws SQLException;

    void upsertPlayer(UUID uuid, String name, long timestamp) throws SQLException;

    Optional<PlayerProfile> findProfile(UUID uuid) throws SQLException;

    Optional<PlayerProfile> findProfileByName(String name) throws SQLException;

    void updateRisk(UUID uuid, int riskScore, long lastSeen) throws SQLException;

    void setAlertsEnabled(UUID uuid, boolean enabled) throws SQLException;

    TimelineEvent addTimelineEvent(TimelineEvent event) throws SQLException;

    TimelineEvent saveRiskAndEvent(int score, TimelineEvent event) throws SQLException;

    Map<String, Integer> miningCounts(UUID uuid, long sinceMillis) throws SQLException;

    boolean hasRecentSignal(UUID uuid, String eventType, String firstDetail, long sinceMillis) throws SQLException;

    List<TimelineEvent> getRecentNotes(UUID uuid, int limit) throws SQLException;

    List<TimelineEvent> getRecentTimeline(UUID uuid, int limit) throws SQLException;

    List<TimelineEvent> getRecentRiskSignals(UUID uuid, int limit) throws SQLException;

    List<PlayerProfile> getTopRiskProfiles(int limit) throws SQLException;

    int countTimelineEvents(UUID uuid, String eventType, String materialName, long sinceMillis) throws SQLException;

    int decayRiskScores(int amount) throws SQLException;

    void purgeOldTimelineEvents(int keepDataDays) throws SQLException;

    @Override
    void close();
}
