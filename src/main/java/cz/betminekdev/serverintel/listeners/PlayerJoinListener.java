package cz.betminekdev.serverintel.listeners;

import cz.betminekdev.serverintel.storage.StorageService;
import cz.betminekdev.serverintel.timeline.TimelineEventType;
import cz.betminekdev.serverintel.timeline.TimelineService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final TimelineService timelineService;

    public PlayerJoinListener(JavaPlugin plugin, StorageService storage, TimelineService timelineService) {
        this.plugin = plugin;
        this.storage = storage;
        this.timelineService = timelineService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            storage.upsertPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), System.currentTimeMillis());
            timelineService.record(event.getPlayer(), TimelineEventType.JOIN, event.getPlayer().getLocation(), 0, "Joined server", "");
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not update ServerIntel profile for " + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }
}
