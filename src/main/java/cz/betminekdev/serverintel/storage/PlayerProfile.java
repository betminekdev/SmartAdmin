package cz.betminekdev.serverintel.storage;

import java.util.UUID;

public record PlayerProfile(
        UUID uuid,
        String name,
        long firstSeen,
        long lastSeen,
        int riskScore,
        boolean alertsEnabled
) {
}
