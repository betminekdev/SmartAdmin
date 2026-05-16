package cz.betminekdev.serverintel.alerts;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.storage.PlayerProfile;
import cz.betminekdev.serverintel.storage.StorageService;
import cz.betminekdev.serverintel.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class AlertService {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final Supplier<ServerIntelConfig> config;
    private final Map<UUID, Long> lastAlertByPlayer = new HashMap<>();

    public AlertService(JavaPlugin plugin, StorageService storage, Supplier<ServerIntelConfig> config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
    }

    public void handleRiskIncrease(Player target, int oldScore, int newScore, String reason) {
        ServerIntelConfig current = config.get();
        if (!current.alertsEnabled() || newScore < current.alertThreshold()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastAlert = lastAlertByPlayer.getOrDefault(target.getUniqueId(), 0L);
        boolean crossedThreshold = oldScore < current.alertThreshold();
        boolean cooldownExpired = now - lastAlert >= current.alertCooldownSeconds() * 1000L;
        if (!crossedThreshold && !cooldownExpired) {
            return;
        }

        lastAlertByPlayer.put(target.getUniqueId(), now);
        String color = newScore >= current.highRiskThreshold() ? "&c" : "&6";
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!canReceiveAlerts(staff)) {
                continue;
            }
            MessageUtil.send(staff, current.prefix(), color + target.getName() + " &7reached Risk " + color + newScore + "/" + current.maxScore() + "&7.");
            MessageUtil.send(staff, "", "&7Reason: &f" + reason);
            MessageUtil.send(staff, "", "&7Actions: &b/sa profile " + target.getName() + " &8| &b/sa timeline " + target.getName() + " &8| &b/sa watch " + target.getName());
        }
    }

    public boolean canReceiveAlerts(Player staff) {
        if (!staff.hasPermission("serverintel.alerts") && !staff.hasPermission("serverintel.admin")) {
            return false;
        }
        try {
            return storage.findProfile(staff.getUniqueId())
                    .map(PlayerProfile::alertsEnabled)
                    .orElse(true);
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not read alert preference for " + staff.getName() + ": " + exception.getMessage());
            return true;
        }
    }

    public void clearCooldowns() {
        lastAlertByPlayer.clear();
    }
}
