package cz.betminekdev.smartadmin.listeners;

import cz.betminekdev.smartadmin.storage.StorageService;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import cz.betminekdev.smartadmin.timeline.TimelineService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            storage.upsertPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), System.currentTimeMillis());
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not update last-seen time for " + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            storage.upsertPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), System.currentTimeMillis());
            timelineService.record(event.getPlayer(), TimelineEventType.JOIN, event.getPlayer().getLocation(), 0, "Joined server", "");
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not update SmartAdmin profile for " + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }
}
