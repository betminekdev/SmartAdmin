package cz.betminekdev.smartadmin.alerts;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.risk.RiskLevel;
import cz.betminekdev.smartadmin.storage.PlayerProfile;
import cz.betminekdev.smartadmin.storage.StorageService;
import cz.betminekdev.smartadmin.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class AlertService {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final Supplier<SmartAdminConfig> config;
    private final Map<UUID, Long> lastAlertByPlayer = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final AtomicInteger pendingRequests = new AtomicInteger();
    private volatile long discordBackoffUntil;
    private boolean warnedEmptyDiscordWebhook;

    public AlertService(JavaPlugin plugin, StorageService storage, Supplier<SmartAdminConfig> config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
    }

    public void handleRiskIncrease(Player target, int oldScore, int newScore, String reason) {
        SmartAdminConfig current = config.get();
        if (!current.alertsEnabled() || newScore <= oldScore || newScore < current.alertThreshold()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastAlert = lastAlertByPlayer.getOrDefault(target.getUniqueId(), 0L);
        boolean cooldownExpired = now - lastAlert >= current.alertCooldownSeconds() * 1000L;
        if (!cooldownExpired) {
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
            MessageUtil.send(staff, "", "&7Actions: &b/sa profile " + target.getName() + " &8| &b/sa evidence " + target.getName() + " &8| &b/sa watch " + target.getName());
        }
        sendDiscordAlert(target.getName(), newScore, reason, current);
    }

    public void warnIfDiscordMisconfigured() {
        SmartAdminConfig current = config.get();
        warnedEmptyDiscordWebhook = false;
        if (current.discordEnabled()) {
            try {
                DiscordWebhook.validate(current.discordWebhookUrl());
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Discord alerts disabled: " + exception.getMessage());
                warnedEmptyDiscordWebhook = true;
            }
        }
    }

    public boolean canReceiveAlerts(Player staff) {
        if (!staff.hasPermission("smartadmin.alerts") && !staff.hasPermission("smartadmin.admin")) {
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

    public void close() {
        lastAlertByPlayer.clear();
        httpClient.shutdownNow();
    }

    private void sendDiscordAlert(String playerName, int riskScore, String reason, SmartAdminConfig current) {
        if (!current.discordEnabled()) {
            return;
        }
        if (warnedEmptyDiscordWebhook || System.currentTimeMillis() < discordBackoffUntil) {
            return;
        }
        if (current.discordHighRiskOnly() && riskScore < current.highRiskThreshold()) {
            return;
        }

        RiskLevel level = RiskLevel.fromScore(riskScore);
        String content = "**SmartAdmin Alert**\n"
                + "Player: " + playerName + "\n"
                + "Risk: " + riskScore + "/" + current.maxScore() + "\n"
                + "Status: " + level.name() + "\n"
                + "Reason: " + reason + "\n"
                + "Suggested: /sa evidence " + playerName;
        String body = DiscordWebhook.payload(content);
        URI endpoint;
        try {
            endpoint = DiscordWebhook.validate(current.discordWebhookUrl());
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (pendingRequests.incrementAndGet() > 4) {
            pendingRequests.decrementAndGet();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 429) {
                    long retrySeconds = 60;
                    try {
                        retrySeconds = Math.max(1, Math.min(3600, (long) Math.ceil(Double.parseDouble(
                                response.headers().firstValue("Retry-After").orElse("60")))));
                    } catch (NumberFormatException ignored) {
                        // Use a conservative backoff for an unrecognized header.
                    }
                    discordBackoffUntil = System.currentTimeMillis() + retrySeconds * 1000L;
                }
                if (response.statusCode() >= 300) {
                    plugin.getLogger().warning("Discord webhook returned HTTP " + response.statusCode() + ".");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                plugin.getLogger().warning("Discord delivery failed (" + exception.getClass().getSimpleName() + "). Check connectivity; webhook URL omitted.");
            } finally {
                pendingRequests.decrementAndGet();
            }
        });
    }
}
