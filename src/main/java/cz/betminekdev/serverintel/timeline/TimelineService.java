package cz.betminekdev.serverintel.timeline;

import cz.betminekdev.serverintel.storage.StorageService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class TimelineService {
    private final StorageService storage;

    public TimelineService(StorageService storage) {
        this.storage = storage;
    }

    public TimelineEvent record(Player player, TimelineEventType type, Location location, int riskChange, String reason, String details) throws SQLException {
        TimelineEvent event = new TimelineEvent(
                0,
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis(),
                type,
                location != null && location.getWorld() != null ? location.getWorld().getName() : null,
                location != null ? location.getBlockX() : null,
                location != null ? location.getBlockY() : null,
                location != null ? location.getBlockZ() : null,
                riskChange,
                reason,
                details == null ? "" : details
        );
        return storage.addTimelineEvent(event);
    }

    public TimelineEvent record(UUID uuid, String name, TimelineEventType type, Location location, int riskChange, String reason, String details) throws SQLException {
        TimelineEvent event = new TimelineEvent(
                0,
                uuid,
                name,
                System.currentTimeMillis(),
                type,
                location != null && location.getWorld() != null ? location.getWorld().getName() : null,
                location != null ? location.getBlockX() : null,
                location != null ? location.getBlockY() : null,
                location != null ? location.getBlockZ() : null,
                riskChange,
                reason,
                details == null ? "" : details
        );
        return storage.addTimelineEvent(event);
    }

    public List<TimelineEvent> recent(UUID uuid, int limit) throws SQLException {
        return storage.getRecentTimeline(uuid, limit);
    }

    public List<TimelineEvent> recentRiskSignals(UUID uuid, int limit) throws SQLException {
        return storage.getRecentRiskSignals(uuid, limit);
    }
}
