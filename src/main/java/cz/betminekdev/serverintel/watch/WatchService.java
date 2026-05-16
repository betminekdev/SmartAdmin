package cz.betminekdev.serverintel.watch;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.timeline.TimelineEvent;
import cz.betminekdev.serverintel.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class WatchService {
    private final Supplier<ServerIntelConfig> config;
    private final Map<UUID, Set<UUID>> watchedByStaff = new HashMap<>();

    public WatchService(Supplier<ServerIntelConfig> config) {
        this.config = config;
    }

    public boolean toggle(Player staff, UUID targetUuid) {
        Set<UUID> watchedPlayers = watchedByStaff.computeIfAbsent(staff.getUniqueId(), ignored -> new HashSet<>());
        if (watchedPlayers.remove(targetUuid)) {
            if (watchedPlayers.isEmpty()) {
                watchedByStaff.remove(staff.getUniqueId());
            }
            return false;
        }
        watchedPlayers.add(targetUuid);
        return true;
    }

    public void notify(TimelineEvent event) {
        if (!config.get().watchEnabled()) {
            return;
        }
        for (Map.Entry<UUID, Set<UUID>> entry : watchedByStaff.entrySet()) {
            if (!entry.getValue().contains(event.playerUuid())) {
                continue;
            }
            Player staff = Bukkit.getPlayer(entry.getKey());
            if (staff == null || !staff.isOnline()) {
                continue;
            }
            String location = event.world() == null ? "" : " at X:" + event.x() + " Y:" + event.y() + " Z:" + event.z();
            String risk = event.riskChange() > 0 ? " &7(+" + event.riskChange() + " risk)" : "";
            MessageUtil.send(staff, "", "&8[&dWatch&8] &f" + event.playerName() + " &7" + event.reason() + location + risk);
        }
    }

    public void clear() {
        watchedByStaff.clear();
    }
}
