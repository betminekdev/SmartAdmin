package cz.betminekdev.serverintel.commands;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.risk.RiskLevel;
import cz.betminekdev.serverintel.storage.PlayerProfile;
import cz.betminekdev.serverintel.storage.StorageService;
import cz.betminekdev.serverintel.timeline.TimelineEvent;
import cz.betminekdev.serverintel.timeline.TimelineService;
import cz.betminekdev.serverintel.util.MessageUtil;
import cz.betminekdev.serverintel.util.TimeUtil;
import cz.betminekdev.serverintel.watch.WatchService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ServerIntelCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final TimelineService timelineService;
    private final WatchService watchService;
    private final Supplier<ServerIntelConfig> config;
    private final Runnable reloadAction;

    public ServerIntelCommand(JavaPlugin plugin, StorageService storage, TimelineService timelineService,
                              WatchService watchService, Supplier<ServerIntelConfig> config, Runnable reloadAction) {
        this.plugin = plugin;
        this.storage = storage;
        this.timelineService = timelineService;
        this.watchService = watchService;
        this.config = config;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return help(sender);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "profile" -> profile(sender, args);
            case "timeline" -> timeline(sender, args);
            case "watch" -> watch(sender, args);
            case "alerts" -> alerts(sender);
            case "reload" -> reload(sender);
            case "version" -> version(sender);
            case "evidence" -> evidence(sender, args);
            default -> {
                MessageUtil.send(sender, config.get().prefix(), "&cUnknown command. Use &f/sa help&c.");
                yield true;
            }
        };
    }

    private boolean help(CommandSender sender) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        MessageUtil.send(sender, config.get().prefix(), "&bServerIntel commands");
        sender.sendMessage(MessageUtil.color("&8- &b/sa profile <player> &7View risk score and recent signals."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa timeline <player> &7View recent investigation timeline."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa watch <player> &7Toggle live watch mode."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa alerts &7Toggle personal staff alerts."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa reload &7Reload configuration."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa version &7Show plugin version."));
        sender.sendMessage(MessageUtil.color("&8- &b/sa evidence <player> &7Prepared investigation summary command."));
        return true;
    }

    private boolean profile(CommandSender sender, String[] args) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa profile <player>");
            return true;
        }
        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        PlayerProfile profile = optionalProfile.get();
        RiskLevel level = RiskLevel.fromScore(profile.riskScore());
        MessageUtil.send(sender, config.get().prefix(), "&bServerIntel Profile: &f" + profile.name());
        sender.sendMessage(MessageUtil.color("&7Risk Score: " + MessageUtil.riskColor(profile.riskScore()) + profile.riskScore() + "/" + config.get().maxScore()));
        sender.sendMessage(MessageUtil.color("&7Status: " + level.color() + level.name()));
        sender.sendMessage(MessageUtil.color("&7Last Seen: &f" + TimeUtil.dateTime(profile.lastSeen())));
        sender.sendMessage(MessageUtil.color("&7Important signals:"));
        try {
            List<TimelineEvent> signals = timelineService.recentRiskSignals(profile.uuid(), 5);
            if (signals.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No important signals stored yet."));
            } else {
                for (TimelineEvent signal : signals) {
                    sender.sendMessage(MessageUtil.color("&8- &f" + signal.reason() + " &7(+" + signal.riskChange() + ")"));
                }
            }
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not load profile signals.");
            plugin.getLogger().warning("Could not load profile signals: " + exception.getMessage());
        }
        sender.sendMessage(MessageUtil.color("&7Recommended action: &fWatch manually. Do not punish without review."));
        return true;
    }

    private boolean timeline(CommandSender sender, String[] args) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa timeline <player>");
            return true;
        }
        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        PlayerProfile profile = optionalProfile.get();
        MessageUtil.send(sender, config.get().prefix(), "&bTimeline: &f" + profile.name());
        try {
            List<TimelineEvent> events = new ArrayList<>(timelineService.recent(profile.uuid(), 10));
            Collections.reverse(events);
            if (events.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No timeline events stored yet."));
                return true;
            }
            for (TimelineEvent event : events) {
                sender.sendMessage(MessageUtil.color(formatTimeline(event)));
            }
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not load timeline.");
            plugin.getLogger().warning("Could not load timeline: " + exception.getMessage());
        }
        return true;
    }

    private boolean watch(CommandSender sender, String[] args) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (!(sender instanceof Player staff)) {
            MessageUtil.send(sender, config.get().prefix(), "&cOnly players can use watch mode.");
            return true;
        }
        if (!config.get().watchEnabled()) {
            MessageUtil.send(sender, config.get().prefix(), "&cWatch mode is disabled in config.");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa watch <player>");
            return true;
        }
        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }
        PlayerProfile profile = optionalProfile.get();
        boolean enabled = watchService.toggle(staff, profile.uuid());
        MessageUtil.send(sender, config.get().prefix(), enabled
                ? "&7Watch mode enabled for &f" + profile.name() + "&7."
                : "&7Watch mode disabled for &f" + profile.name() + "&7.");
        return true;
    }

    private boolean alerts(CommandSender sender) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.get().prefix(), "&cOnly players can toggle personal alerts.");
            return true;
        }
        try {
            storage.upsertPlayer(player.getUniqueId(), player.getName(), System.currentTimeMillis());
            PlayerProfile profile = storage.findProfile(player.getUniqueId()).orElseThrow();
            boolean enabled = !profile.alertsEnabled();
            storage.setAlertsEnabled(player.getUniqueId(), enabled);
            MessageUtil.send(sender, config.get().prefix(), enabled ? "&7Personal staff alerts enabled." : "&7Personal staff alerts disabled.");
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not update alert preference.");
            plugin.getLogger().warning("Could not update alert preference: " + exception.getMessage());
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("serverintel.reload") && !sender.hasPermission("serverintel.admin")) {
            noPermission(sender);
            return true;
        }
        reloadAction.run();
        MessageUtil.send(sender, config.get().prefix(), "&aConfiguration reloaded.");
        return true;
    }

    private boolean version(CommandSender sender) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        MessageUtil.send(sender, config.get().prefix(), "&bServerIntel &f" + plugin.getDescription().getVersion() + " &7- smart staff assistant.");
        return true;
    }

    private boolean evidence(CommandSender sender, String[] args) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa evidence <player>");
            return true;
        }
        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }
        PlayerProfile profile = optionalProfile.get();
        MessageUtil.send(sender, config.get().prefix(), "&bEvidence summary for &f" + profile.name());
        sender.sendMessage(MessageUtil.color("&7This command is prepared for a future report generator."));
        sender.sendMessage(MessageUtil.color("&7For now, use &b/sa profile " + profile.name() + " &7and &b/sa timeline " + profile.name() + "&7."));
        return true;
    }

    private Optional<PlayerProfile> findProfile(String name) {
        Player online = Bukkit.getPlayerExact(name);
        try {
            if (online != null) {
                storage.upsertPlayer(online.getUniqueId(), online.getName(), System.currentTimeMillis());
                return storage.findProfile(online.getUniqueId());
            }
            Optional<PlayerProfile> stored = storage.findProfileByName(name);
            if (stored.isPresent()) {
                return stored;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (offline.hasPlayedBefore() && offline.getName() != null) {
                return storage.findProfile(offline.getUniqueId());
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not resolve ServerIntel profile for " + name + ": " + exception.getMessage());
        }
        return Optional.empty();
    }

    private String formatTimeline(TimelineEvent event) {
        StringBuilder line = new StringBuilder("&8[&7")
                .append(TimeUtil.time(event.timestamp()))
                .append("&8] &f")
                .append(event.reason());
        if (event.world() != null && event.x() != null && event.y() != null && event.z() != null) {
            line.append(" &7at ").append(event.world())
                    .append(" - X:").append(event.x())
                    .append(" Y:").append(event.y())
                    .append(" Z:").append(event.z());
        }
        if (event.riskChange() > 0) {
            line.append(" &8(").append("&c+").append(event.riskChange()).append(" risk&8)");
        }
        return line.toString();
    }

    private boolean hasStaff(CommandSender sender) {
        return sender.hasPermission("serverintel.staff") || sender.hasPermission("serverintel.admin");
    }

    private void noPermission(CommandSender sender) {
        MessageUtil.send(sender, config.get().prefix(), "&cYou do not have permission to use this command.");
    }

    private void playerNotFound(CommandSender sender) {
        MessageUtil.send(sender, config.get().prefix(), "&cPlayer not found or has no ServerIntel profile yet.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasStaff(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("help", "profile", "timeline", "watch", "alerts", "reload", "version", "evidence"), args[0]);
        }
        if (args.length == 2 && List.of("profile", "timeline", "watch", "evidence").contains(args[0].toLowerCase(Locale.ROOT))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.startsWith(lowerPrefix))
                .toList();
    }
}
