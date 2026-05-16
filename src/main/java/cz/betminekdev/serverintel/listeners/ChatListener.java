package cz.betminekdev.serverintel.listeners;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.risk.RiskService;
import cz.betminekdev.serverintel.timeline.TimelineEventType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public final class ChatListener implements Listener {
    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.|discord\\.gg/|\\.ru\\b|\\.xyz\\b)");

    private final JavaPlugin plugin;
    private final RiskService riskService;
    private final Supplier<ServerIntelConfig> config;
    private final Map<UUID, ArrayDeque<Long>> recentMessages = new HashMap<>();

    public ChatListener(JavaPlugin plugin, RiskService riskService, Supplier<ServerIntelConfig> config) {
        this.plugin = plugin;
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        ServerIntelConfig current = config.get();
        if (!current.chatEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = event.getPlayer().getUniqueId();
        ArrayDeque<Long> timestamps = recentMessages.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        timestamps.addLast(now);
        long cutoff = now - current.spamWindowSeconds() * 1000L;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.removeFirst();
        }

        boolean spam = timestamps.size() == current.spamMessageCount();
        boolean suspiciousLink = LINK_PATTERN.matcher(event.getMessage()).find();
        if (!spam && !suspiciousLink) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (spam) {
                riskService.addSignal(event.getPlayer(), TimelineEventType.CHAT_SIGNAL, event.getPlayer().getLocation(),
                        current.spamRisk(), "Chat spam", "messages=" + timestamps.size() + "; windowSeconds=" + current.spamWindowSeconds());
            }
            if (suspiciousLink) {
                riskService.addSignal(event.getPlayer(), TimelineEventType.CHAT_SIGNAL, event.getPlayer().getLocation(),
                        current.suspiciousLinkRisk(), "Suspicious link in chat", "messageLength=" + event.getMessage().length());
            }
        });
    }
}
