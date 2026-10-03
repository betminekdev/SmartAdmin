package cz.betminekdev.smartadmin.commands;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.risk.RiskLevel;
import cz.betminekdev.smartadmin.storage.PlayerProfile;
import cz.betminekdev.smartadmin.storage.StorageService;
import cz.betminekdev.smartadmin.timeline.TimelineEvent;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import cz.betminekdev.smartadmin.timeline.TimelineService;
import cz.betminekdev.smartadmin.util.MessageUtil;
import cz.betminekdev.smartadmin.util.TimeUtil;
import cz.betminekdev.smartadmin.util.EvidenceFiles;
import cz.betminekdev.smartadmin.watch.WatchService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;

public final class SmartAdminCommand implements CommandExecutor, TabCompleter {
    private static final int DEFAULT_TIMELINE_LIMIT = 10;
    private static final int MAX_TIMELINE_LIMIT = 30;
    private static final int MAX_TIMELINE_PAGE = 1000;

    private final JavaPlugin plugin;
    private final StorageService storage;
    private final TimelineService timelineService;
    private final WatchService watchService;
    private final Supplier<SmartAdminConfig> config;
    private final Runnable reloadAction;
    private final AtomicInteger pendingExports = new AtomicInteger();

    public SmartAdminCommand(JavaPlugin plugin, StorageService storage, TimelineService timelineService,
                              WatchService watchService, Supplier<SmartAdminConfig> config, Runnable reloadAction) {
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
            case "evidence" -> evidence(sender, args);
            case "export" -> export(sender, args);
            case "top" -> top(sender, args);
            case "watch" -> watch(sender, args);
            case "alerts" -> alerts(sender);
            case "note" -> note(sender, args);
            case "reset" -> reset(sender, args);
            case "reload" -> reload(sender);
            case "version" -> version(sender);
            default -> {
                MessageUtil.send(sender, config.get().prefix(), "&cUnknown command. Use &f/sa help&c.");
                yield true;
            }
        };
    }

    private boolean help(CommandSender sender) {
        if (!hasAnyCommandPermission(sender)) {
            noPermission(sender);
            return true;
        }
        MessageUtil.send(sender, config.get().prefix(), "&bSmartAdmin &f" + plugin.getDescription().getVersion());
        sender.sendMessage(MessageUtil.color("&7Smart staff assistant for Minecraft servers"));
        sender.sendMessage(MessageUtil.color("&8&m-----------------------------------------------------"));
        sender.sendMessage(MessageUtil.color("&7Commands:"));
        helpLine(sender, "profile", "<player>", "show player profile");
        helpLine(sender, "timeline", "<player> [limit] [page]", "browse events, newest page first");
        helpLine(sender, "evidence", "<player>", "show investigation summary");
        helpLine(sender, "export", "<player>", "export evidence report");
        helpLine(sender, "top", "[limit]", "show highest risk players");
        helpLine(sender, "watch", "<player>", "toggle watch mode");
        helpLine(sender, "watch", "list|clear", "list or clear your watches");
        helpLine(sender, "alerts", "", "toggle personal alerts");
        helpLine(sender, "note", "<player> <message>", "add staff note");
        helpLine(sender, "reset", "<player>", "reset risk score");
        helpLine(sender, "reload", "", "reload config");
        helpLine(sender, "version", "", "show version");
        sender.sendMessage(MessageUtil.color("&7Aliases: &b/smartadmin&7, &b/sa&7, &b/si"));
        sender.sendMessage(MessageUtil.color("&7Notice: &fSmartAdmin is not an anti-cheat. Review signals manually."));
        sender.sendMessage(MessageUtil.color("&eBeta: test configuration changes on a staging server first."));
        return true;
    }

    private void helpLine(CommandSender sender, String sub, String arguments, String description) {
        if (canUse(sender, sub)) {
            sender.sendMessage(MessageUtil.color("&8- &b/sa " + sub + (arguments.isEmpty() ? "" : " " + arguments) + " &7- " + description));
        }
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
        MessageUtil.send(sender, config.get().prefix(), "&bSmartAdmin Profile: &f" + profile.name());
        sender.sendMessage(MessageUtil.color("&7Risk Score: " + MessageUtil.riskColor(profile.riskScore()) + profile.riskScore() + "/" + config.get().maxScore()));
        sender.sendMessage(MessageUtil.color("&7Status: " + level.color() + level.name()));
        sender.sendMessage(MessageUtil.color("&7First Seen: &f" + TimeUtil.dateTime(profile.firstSeen())));
        sender.sendMessage(MessageUtil.color("&7Last Seen: &f" + TimeUtil.dateTime(profile.lastSeen())));
        try {
            List<TimelineEvent> signals = timelineService.recentRiskSignals(profile.uuid(), 3);
            sender.sendMessage(MessageUtil.color("&7Recent positive-risk signals (historical, not a score breakdown):"));
            if (signals.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No positive-risk signals stored."));
            }
            for (TimelineEvent signal : signals) {
                sender.sendMessage(MessageUtil.color(formatTimeline(signal)));
            }
        } catch (SQLException exception) {
            sender.sendMessage(MessageUtil.color("&7Recent important signals: &cCould not load"));
            plugin.getLogger().warning("Could not load profile signal count: " + exception.getMessage());
        }
        sender.sendMessage(MessageUtil.color("&7Next: &b/sa timeline " + profile.name() + " &8| &b/sa evidence " + profile.name() + " &8| &b/sa watch " + profile.name()));
        sender.sendMessage(MessageUtil.color("&7Recommended action: &fReview manually. Do not punish without confirmation."));
        return true;
    }

    private boolean timeline(CommandSender sender, String[] args) {
        if (!hasStaff(sender)) {
            noPermission(sender);
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa timeline <player> [limit] [page]");
            return true;
        }
        int limit = DEFAULT_TIMELINE_LIMIT;
        if (args.length >= 3) {
            Optional<Integer> parsedLimit = parsePositiveInt(args[2]);
            if (parsedLimit.isEmpty()) {
                MessageUtil.send(sender, config.get().prefix(), "&cTimeline limit must be a number from 1 to " + MAX_TIMELINE_LIMIT + ".");
                return true;
            }
            limit = Math.max(1, Math.min(MAX_TIMELINE_LIMIT, parsedLimit.get()));
        }

        int page = 1;
        if (args.length >= 4) {
            Optional<Integer> parsedPage = parsePositiveInt(args[3]);
            if (parsedPage.isEmpty() || parsedPage.get() > MAX_TIMELINE_PAGE) {
                MessageUtil.send(sender, config.get().prefix(), "&cTimeline page must be a number from 1 to " + MAX_TIMELINE_PAGE + ".");
                return true;
            }
            page = parsedPage.get();
        }

        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        PlayerProfile profile = optionalProfile.get();
        MessageUtil.send(sender, config.get().prefix(), "&bTimeline: &f" + profile.name() + " &7(page " + page + ", up to " + limit + " events)");
        try {
            List<TimelineEvent> events = new ArrayList<>(storage.getTimelinePage(profile.uuid(), limit, page));
            boolean hasOlder = events.size() > limit;
            if (hasOlder) {
                events.removeLast();
            }
            Collections.reverse(events);
            if (events.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No events on this page. Try page 1 for the latest events."));
            }
            for (TimelineEvent event : events) {
                sender.sendMessage(MessageUtil.color(formatTimeline(event)));
            }
            String navigation = "/sa timeline " + profile.uuid() + " " + limit + " ";
            if (page > 1) {
                sender.sendMessage(MessageUtil.color("&7Newer: &b" + navigation + (page - 1)));
            }
            if (hasOlder && page < MAX_TIMELINE_PAGE) {
                sender.sendMessage(MessageUtil.color("&7Older: &b" + navigation + (page + 1)));
            }
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not load timeline.");
            plugin.getLogger().warning("Could not load timeline: " + exception.getMessage());
        }
        return true;
    }

    private boolean evidence(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smartadmin.evidence") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        if (!config.get().evidenceEnabled()) {
            MessageUtil.send(sender, config.get().prefix(), "&cEvidence reports are disabled in config.");
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

        try {
            EvidenceData report = loadEvidence(optionalProfile.get());
            sendEvidence(sender, report);
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not generate evidence report.");
            plugin.getLogger().warning("Could not generate evidence report: " + exception.getMessage());
        }
        return true;
    }

    private boolean export(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smartadmin.export") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        if (!config.get().exportEnabled()) {
            MessageUtil.send(sender, config.get().prefix(), "&cEvidence export is disabled in config.");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa export <player>");
            return true;
        }
        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        try {
            EvidenceData report = loadEvidence(optionalProfile.get());
            Path folder = Path.of(config.get().exportFolder());
            List<String> lines = List.copyOf(buildExportLines(report));
            if (pendingExports.incrementAndGet() > 4) {
                pendingExports.decrementAndGet();
                MessageUtil.send(sender, config.get().prefix(), "&cExport queue is busy. Try again shortly.");
                return true;
            }
            MessageUtil.send(sender, config.get().prefix(), "&7Export queued for &f" + report.profile().name() + "&7.");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String result;
                try {
                    Path exported = EvidenceFiles.write(folder, report.profile().name(), lines);
                    result = "&aEvidence report exported: &f" + exported;
                } catch (IOException exception) {
                    result = "&cCould not write evidence report. Check the export folder and free disk space.";
                    plugin.getLogger().warning("Evidence export failed: " + exception.getClass().getSimpleName());
                } finally {
                    pendingExports.decrementAndGet();
                }
                String message = result;
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (canUse(sender, "export") && (!(sender instanceof Player player) || player.isOnline())) {
                            MessageUtil.send(sender, config.get().prefix(), message);
                        }
                    });
                } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
                    // The report may finish during shutdown; do not schedule a callback on a disabled plugin.
                }
            });
        } catch (SQLException | InvalidPathException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not export evidence report.");
            plugin.getLogger().warning("Could not export SmartAdmin evidence report: " + exception.getMessage());
        }
        return true;
    }

    private boolean top(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smartadmin.top") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        int limit = config.get().topDefaultLimit();
        if (args.length >= 2) {
            Optional<Integer> parsedLimit = parsePositiveInt(args[1]);
            if (parsedLimit.isEmpty()) {
                MessageUtil.send(sender, config.get().prefix(), "&cTop limit must be a number from 1 to " + config.get().topMaxLimit() + ".");
                return true;
            }
            limit = Math.max(1, Math.min(config.get().topMaxLimit(), parsedLimit.get()));
        }

        try {
            List<PlayerProfile> profiles = storage.getTopRiskProfiles(limit);
            MessageUtil.send(sender, config.get().prefix(), "&bSmartAdmin Top Risk Players");
            if (profiles.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No players with risk score above 0."));
                return true;
            }
            int index = 1;
            for (PlayerProfile profile : profiles) {
                RiskLevel level = RiskLevel.fromScore(profile.riskScore());
                sender.sendMessage(MessageUtil.color("&7" + index + ". &f" + profile.name() + " &7- " + MessageUtil.riskColor(profile.riskScore()) + profile.riskScore() + "/" + config.get().maxScore() + " " + level.color() + level.name()));
                index++;
            }
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not load top risk players.");
            plugin.getLogger().warning("Could not load top risk players: " + exception.getMessage());
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
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa watch <player>|list|clear");
            return true;
        }
        if (args[1].equalsIgnoreCase("clear")) {
            int removed = watchService.clear(staff.getUniqueId());
            MessageUtil.send(sender, config.get().prefix(), "&aCleared " + removed + " watched player(s). Other staff watches are unchanged.");
            return true;
        }
        if (args[1].equalsIgnoreCase("list")) {
            try {
                List<String> watched = new ArrayList<>();
                for (UUID uuid : watchService.watchedPlayers(staff.getUniqueId())) {
                    watched.add(storage.findProfile(uuid).map(PlayerProfile::name).orElse(uuid.toString()));
                }
                watched.sort(String.CASE_INSENSITIVE_ORDER);
                MessageUtil.send(sender, config.get().prefix(), "&bYour watches: &f" + watched.size()
                        + (config.get().watchEnabled() ? "" : " &7(delivery disabled in config)"));
                if (watched.isEmpty()) {
                    sender.sendMessage(MessageUtil.color("&7No players watched. Use /sa watch <player>."));
                }
                for (String name : watched) {
                    sender.sendMessage(MessageUtil.color("&8- &f" + name));
                }
            } catch (SQLException exception) {
                MessageUtil.send(sender, config.get().prefix(), "&cCould not load watched player names.");
                plugin.getLogger().warning("Could not load watch list: " + exception.getMessage());
            }
            return true;
        }
        if (!config.get().watchEnabled()) {
            MessageUtil.send(sender, config.get().prefix(), "&cWatch mode is disabled in config.");
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
        if (!hasStaff(sender) && !sender.hasPermission("smartadmin.alerts")) {
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

    private boolean note(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smartadmin.note") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa note <player> <message>");
            return true;
        }

        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        String note = EvidenceFiles.plainText(joinArgs(args, 2));
        if (note.isEmpty()) {
            MessageUtil.send(sender, config.get().prefix(), "&cNote text cannot be empty.");
            return true;
        }
        int maxLength = config.get().noteMaxLength();
        if (note.length() > maxLength) {
            MessageUtil.send(sender, config.get().prefix(), "&cNote is too long. Maximum length is &f" + maxLength + " &ccharacters.");
            return true;
        }

        PlayerProfile profile = optionalProfile.get();
        try {
            TimelineEvent saved = timelineService.record(profile.uuid(), profile.name(), TimelineEventType.STAFF_NOTE, null, 0, "Staff note by " + sender.getName(), note);
            watchService.notify(saved);
            MessageUtil.send(sender, config.get().prefix(), "&aStaff note added for &f" + profile.name() + "&a.");
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not add staff note.");
            plugin.getLogger().warning("Could not add SmartAdmin staff note for " + profile.name() + ": " + exception.getMessage());
        }
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smartadmin.reset") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, config.get().prefix(), "&cUsage: /sa reset <player>");
            return true;
        }

        Optional<PlayerProfile> optionalProfile = findProfile(args[1]);
        if (optionalProfile.isEmpty()) {
            playerNotFound(sender);
            return true;
        }

        PlayerProfile profile = optionalProfile.get();
        try {
            long now = System.currentTimeMillis();
            TimelineEvent saved = storage.saveRiskAndEvent(0, new TimelineEvent(0, profile.uuid(), profile.name(), now,
                    TimelineEventType.STAFF_ACTION, null, null, null, null, 0,
                    "Risk score reset by " + sender.getName(), "oldScore=" + profile.riskScore() + "; newScore=0"));
            watchService.notify(saved);
            MessageUtil.send(sender, config.get().prefix(), "&aRisk score reset for &f" + profile.name() + "&a.");
        } catch (SQLException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&cCould not reset player risk score.");
            plugin.getLogger().warning("Could not reset SmartAdmin risk score for " + profile.name() + ": " + exception.getMessage());
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("smartadmin.reload") && !sender.hasPermission("smartadmin.admin")) {
            noPermission(sender);
            return true;
        }
        try {
            reloadAction.run();
            MessageUtil.send(sender, config.get().prefix(), "&aConfiguration reloaded.");
        } catch (IllegalStateException exception) {
            MessageUtil.send(sender, config.get().prefix(), "&c" + exception.getMessage());
        }
        return true;
    }

    private boolean version(CommandSender sender) {
        if (!hasAnyCommandPermission(sender)) {
            noPermission(sender);
            return true;
        }
        MessageUtil.send(sender, config.get().prefix(), "&bSmartAdmin &f" + plugin.getDescription().getVersion() + " &7- smart staff assistant.");
        return true;
    }

    private EvidenceData loadEvidence(PlayerProfile profile) throws SQLException {
        List<TimelineEvent> signals = timelineService.recentRiskSignals(profile.uuid(), 5);
        List<TimelineEvent> timeline = timelineService.recent(profile.uuid(), config.get().evidenceMaxTimelineEvents());
        List<TimelineEvent> notes = storage.getRecentNotes(profile.uuid(), 10);
        return new EvidenceData(profile, RiskLevel.fromScore(profile.riskScore()), signals, timeline, notes);
    }

    private void sendEvidence(CommandSender sender, EvidenceData report) {
        PlayerProfile profile = report.profile();
        MessageUtil.send(sender, config.get().prefix(), "&bSmartAdmin Evidence Report: &f" + profile.name());
        sender.sendMessage(MessageUtil.color("&7Risk Score: " + MessageUtil.riskColor(profile.riskScore()) + profile.riskScore() + "/" + config.get().maxScore()));
        sender.sendMessage(MessageUtil.color("&7Status: " + report.level().color() + report.level().name()));

        if (report.signals().isEmpty() && report.timeline().isEmpty()) {
            sender.sendMessage(MessageUtil.color("&7Not enough investigation data available yet."));
        } else {
            sender.sendMessage(MessageUtil.color("&7Top Signals:"));
            if (report.signals().isEmpty()) {
                sender.sendMessage(MessageUtil.color("&8- &7No suspicious signals stored yet."));
            } else {
                for (TimelineEvent signal : report.signals()) {
                    sender.sendMessage(MessageUtil.color("&8- &f" + signal.reason() + riskSuffix(signal)));
                }
            }
            sender.sendMessage(MessageUtil.color("&7Recent Timeline:"));
            List<TimelineEvent> events = new ArrayList<>(report.timeline());
            Collections.reverse(events);
            for (TimelineEvent event : events) {
                sender.sendMessage(MessageUtil.color(formatTimeline(event)));
            }
        }

        if (!report.notes().isEmpty()) {
            sender.sendMessage(MessageUtil.color("&7Recent Staff Notes:"));
            for (TimelineEvent note : report.notes()) {
                sender.sendMessage(MessageUtil.color("&8- &f" + EvidenceFiles.plainText(note.details())));
            }
        }
        if (config.get().evidenceIncludeRecommendation()) {
            sender.sendMessage(MessageUtil.color("&7Recommendation: &fManual review recommended. Do not punish without staff confirmation."));
        }
    }

    private List<String> buildExportLines(EvidenceData report) {
        PlayerProfile profile = report.profile();
        ArrayList<String> lines = new ArrayList<>();
        lines.add("SmartAdmin Evidence Report");
        lines.add("Generated: " + TimeUtil.dateTime(System.currentTimeMillis()));
        lines.add("");
        lines.add("Player: " + profile.name());
        lines.add("UUID: " + profile.uuid());
        lines.add("Risk Score: " + profile.riskScore() + "/" + config.get().maxScore());
        lines.add("Risk Level: " + report.level().name());
        lines.add("");
        lines.add("Top Signals:");
        if (report.signals().isEmpty()) {
            lines.add("- No suspicious signals stored yet.");
        } else {
            for (TimelineEvent signal : report.signals()) {
                lines.add("- " + signal.reason() + plainRiskSuffix(signal));
            }
        }
        lines.add("");
        lines.add("Recent Timeline:");
        List<TimelineEvent> events = new ArrayList<>(report.timeline());
        Collections.reverse(events);
        if (events.isEmpty()) {
            lines.add("- Not enough investigation data available yet.");
        } else {
            for (TimelineEvent event : events) {
                lines.add(formatPlainTimeline(event));
            }
        }
        lines.add("");
        lines.add("Staff Notes:");
        if (report.notes().isEmpty()) {
            lines.add("- No retained staff notes.");
        } else {
            for (TimelineEvent note : report.notes()) {
                lines.add("- [" + TimeUtil.dateTime(note.timestamp()) + "] " + note.reason() + ": " + EvidenceFiles.plainText(note.details()));
            }
        }
        lines.add("");
        lines.add("Recommendation: Manual review recommended. Do not punish without staff confirmation.");
        lines.add("Disclaimer: This report is not proof of cheating. SmartAdmin provides server-side signals for staff review.");
        return lines;
    }

    private Optional<PlayerProfile> findProfile(String name) {
        Player online = Bukkit.getPlayerExact(name);
        try {
            if (online != null) {
                return storage.findProfile(online.getUniqueId());
            }
            Optional<PlayerProfile> stored = storage.findProfileByName(name);
            if (stored.isPresent()) {
                return stored;
            }
            try {
                return storage.findProfile(UUID.fromString(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown names are not resolved through a blocking external profile lookup.
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not resolve SmartAdmin profile for " + name + ": " + exception.getMessage());
        }
        return Optional.empty();
    }

    private String formatTimeline(TimelineEvent event) {
        StringBuilder line = new StringBuilder("&8[&7")
                .append(TimeUtil.dateTime(event.timestamp()))
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
        if (event.eventType() == TimelineEventType.STAFF_NOTE && event.details() != null && !event.details().isBlank()) {
            line.append(" &7- &f").append(EvidenceFiles.plainText(event.details()));
        }
        return line.toString();
    }

    private String formatPlainTimeline(TimelineEvent event) {
        StringBuilder line = new StringBuilder("[")
                .append(TimeUtil.dateTime(event.timestamp()))
                .append("] ")
                .append(event.reason());
        if (event.world() != null && event.x() != null && event.y() != null && event.z() != null) {
            line.append(" at ").append(event.world())
                    .append(" - X:").append(event.x())
                    .append(" Y:").append(event.y())
                    .append(" Z:").append(event.z());
        }
        if (event.riskChange() > 0) {
            line.append(" (+").append(event.riskChange()).append(" risk)");
        }
        if (event.eventType() == TimelineEventType.STAFF_NOTE && event.details() != null && !event.details().isBlank()) {
            line.append(" - ").append(EvidenceFiles.plainText(event.details()));
        }
        return line.toString();
    }

    private String riskSuffix(TimelineEvent event) {
        return event.riskChange() > 0 ? " &7(+" + event.riskChange() + ")" : "";
    }

    private String plainRiskSuffix(TimelineEvent event) {
        return event.riskChange() > 0 ? " (+" + event.riskChange() + ")" : "";
    }

    private Optional<Integer> parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean hasStaff(CommandSender sender) {
        return sender.hasPermission("smartadmin.staff") || sender.hasPermission("smartadmin.admin");
    }

    private boolean hasAnyCommandPermission(CommandSender sender) {
        return sender.hasPermission("smartadmin.admin")
                || sender.hasPermission("smartadmin.staff")
                || sender.hasPermission("smartadmin.alerts")
                || sender.hasPermission("smartadmin.reload")
                || sender.hasPermission("smartadmin.reset")
                || sender.hasPermission("smartadmin.note")
                || sender.hasPermission("smartadmin.evidence")
                || sender.hasPermission("smartadmin.export")
                || sender.hasPermission("smartadmin.top");
    }

    private void noPermission(CommandSender sender) {
        MessageUtil.send(sender, config.get().prefix(), "&cYou do not have permission to use this command.");
    }

    private void playerNotFound(CommandSender sender) {
        MessageUtil.send(sender, config.get().prefix(), "&cPlayer not found or has no SmartAdmin profile yet.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasAnyCommandPermission(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("help", "profile", "timeline", "evidence", "export", "top", "watch", "alerts", "note", "reset", "reload", "version")
                    .stream().filter(sub -> canUse(sender, sub)).toList(), args[0]);
        }
        if (args.length == 2 && canUse(sender, args[0].toLowerCase(Locale.ROOT))
                && List.of("profile", "timeline", "watch", "evidence", "export", "reset", "note").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> targets = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> targets.add(player.getName()));
            if (args[0].equalsIgnoreCase("watch")) {
                targets.addAll(List.of("list", "clear"));
            }
            return targets.stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .distinct()
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

    private boolean canUse(CommandSender sender, String sub) {
        return switch (sub) {
            case "profile", "timeline", "watch" -> hasStaff(sender);
            case "alerts" -> hasStaff(sender) || sender.hasPermission("smartadmin.alerts");
            case "help", "version" -> hasAnyCommandPermission(sender);
            default -> sender.hasPermission("smartadmin.admin") || sender.hasPermission("smartadmin." + sub);
        };
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (index > startIndex) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }

    private record EvidenceData(
            PlayerProfile profile,
            RiskLevel level,
            List<TimelineEvent> signals,
            List<TimelineEvent> timeline,
            List<TimelineEvent> notes
    ) {
    }
}
