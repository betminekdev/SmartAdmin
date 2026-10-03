package cz.betminekdev.smartadmin.watch;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.timeline.TimelineEvent;
import cz.betminekdev.smartadmin.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class WatchService implements Listener {
    private final Supplier<SmartAdminConfig> config;
    private final Map<UUID, Set<UUID>> watchedByStaff = new HashMap<>();

    public WatchService(Supplier<SmartAdminConfig> config) {
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
            if (staff == null || !staff.isOnline()
                    || (!staff.hasPermission("smartadmin.staff") && !staff.hasPermission("smartadmin.admin"))) {
                continue;
            }
            String location = event.world() == null ? "" : " at " + event.world() + " X:" + event.x() + " Y:" + event.y() + " Z:" + event.z();
            String risk = event.riskChange() > 0 ? " &7(+" + event.riskChange() + " risk)" : "";
            MessageUtil.send(staff, "", "&8[&dWatch&8] &f" + event.playerName() + " &7" + event.reason() + location + risk);
        }
    }

    public void clear() {
        watchedByStaff.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        watchedByStaff.remove(event.getPlayer().getUniqueId());
    }
}
