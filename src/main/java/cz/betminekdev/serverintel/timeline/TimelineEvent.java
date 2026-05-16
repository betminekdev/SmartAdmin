package cz.betminekdev.serverintel.timeline;

import java.util.UUID;

public record TimelineEvent(
        long id,
        UUID playerUuid,
        String playerName,
        long timestamp,
        TimelineEventType eventType,
        String world,
        Integer x,
        Integer y,
        Integer z,
        int riskChange,
        String reason,
        String details
) {
    public TimelineEvent withId(long newId) {
        return new TimelineEvent(newId, playerUuid, playerName, timestamp, eventType, world, x, y, z, riskChange, reason, details);
    }
}
