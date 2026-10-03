package cz.betminekdev.smartadmin.listeners;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.risk.RiskService;
import cz.betminekdev.smartadmin.risk.SignalWindow;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

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
    private final Supplier<SmartAdminConfig> config;
    private final Map<UUID, SignalWindow> recentMessages = new HashMap<>();
    private final Map<UUID, Long> lastLink = new HashMap<>();

    public ChatListener(JavaPlugin plugin, RiskService riskService, Supplier<SmartAdminConfig> config) {
        this.plugin = plugin;
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String message = event.getMessage();
        long now = System.currentTimeMillis();
        // Capture event data only; all mutable state and Bukkit lookups stay on the server thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            SmartAdminConfig current = config.get();
            Player player = Bukkit.getPlayer(uuid);
            if (!current.chatEnabled() || player == null || !player.isOnline()) {
                return;
            }
            long window = current.spamWindowSeconds() * 1000L;
            boolean spam = recentMessages.computeIfAbsent(uuid, ignored -> new SignalWindow())
                    .record(now, window, current.spamMessageCount());
            if (spam) {
                riskService.addSignal(player, TimelineEventType.CHAT_SIGNAL, player.getLocation(),
                        current.spamRisk(), "High chat message rate", "threshold=" + current.spamMessageCount() + "; windowSeconds=" + current.spamWindowSeconds());
            }
            if (LINK_PATTERN.matcher(message).find() && now - lastLink.getOrDefault(uuid, 0L) >= window) {
                lastLink.put(uuid, now);
                riskService.addSignal(player, TimelineEventType.CHAT_SIGNAL, player.getLocation(),
                        current.suspiciousLinkRisk(), "Link in chat; manual review recommended", "messageLength=" + message.length());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        recentMessages.remove(event.getPlayer().getUniqueId());
        lastLink.remove(event.getPlayer().getUniqueId());
    }
}
