package cz.betminekdev.smartadmin;

import com.google.gson.JsonParser;
import cz.betminekdev.smartadmin.alerts.AlertService;
import cz.betminekdev.smartadmin.alerts.DiscordWebhook;
import cz.betminekdev.smartadmin.commands.SmartAdminCommand;
import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.listeners.MiningListener;
import cz.betminekdev.smartadmin.risk.RiskService;
import cz.betminekdev.smartadmin.risk.SignalWindow;
import cz.betminekdev.smartadmin.storage.SQLiteStorageService;
import cz.betminekdev.smartadmin.timeline.TimelineEvent;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import cz.betminekdev.smartadmin.timeline.TimelineService;
import cz.betminekdev.smartadmin.util.EvidenceFiles;
import cz.betminekdev.smartadmin.watch.WatchService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

final class RegressionTests {
    private static int checks;

    static void run() throws Exception {
        yamlAndConfig();
        windows();
        discord();
        permissions();
        watchWorkflow();
        Path directory = Files.createTempDirectory("smartadmin-tests-");
        try {
            exports(directory);
            storageAndMining(directory);
            timelinePages(directory);
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
        System.out.println("Verified " + checks + " regression checks (including real SQLite and mining listener).");
    }

    private static void yamlAndConfig() throws Exception {
        YamlConfiguration plugin = new YamlConfiguration();
        plugin.load("src/main/resources/plugin.yml");
        equal("SmartAdmin", plugin.getString("name"), "plugin branding");
        equal(List.of("sa", "si"), plugin.getStringList("commands.smartadmin.aliases"), "command aliases");
        equal("0.4.0-beta", plugin.getString("version"), "release version");
        check(Files.readString(Path.of("build.gradle.kts")).contains("version = \"" + plugin.getString("version") + "\""), "Gradle version matches descriptor");
        Map<?, ?> workflow = new Yaml().load(Files.readString(Path.of(".github/workflows/build.yml")));
        check(workflow.get("jobs") instanceof Map, "workflow jobs map");
        YamlConfiguration yaml = defaults();
        equal(9, SmartAdminConfig.load(yaml).valuableOres().size(), "ore configuration");
        yaml.set("risk.max-score", Integer.MAX_VALUE);
        yaml.set("alerts.threshold", Integer.MAX_VALUE);
        yaml.set("notes.max-length", Integer.MAX_VALUE);
        yaml.set("top.max-limit", Integer.MAX_VALUE);
        SmartAdminConfig bounded = SmartAdminConfig.load(yaml);
        equal(100, bounded.maxScore(), "risk hard cap");
        equal(100, bounded.alertThreshold(), "alert cap");
        equal(2000, bounded.noteMaxLength(), "note cap");
        equal(100, bounded.topMaxLimit(), "top cap");
    }

    private static void windows() {
        SignalWindow window = new SignalWindow();
        check(!window.record(1000, 8000, 3), "below chat threshold");
        check(!window.record(2000, 8000, 3), "below threshold second message");
        check(window.record(3000, 8000, 3), "chat threshold fires");
        for (int i = 0; i < 10000; i++) {
            if (window.record(4000, 8000, 3)) {
                throw new AssertionError("Chat cooldown failed under burst");
            }
        }
        check(window.record(11000, 8000, 3), "chat can signal after cooldown");
        check(!window.record(30000, 8000, 3), "expired chat history removed");
    }

    private static void discord() {
        equal("discord.com", DiscordWebhook.validate("https://discord.com/api/webhooks/123/token_abc").getHost(), "Discord endpoint accepted");
        for (String url : List.of("http://discord.com/api/webhooks/1/token", "https://localhost/api/webhooks/1/token",
                "https://discord.com.evil.test/api/webhooks/1/token", "https://discord.com@localhost/api/webhooks/1/token",
                "https://discord.com/api/webhooks/1/token?wait=true", "https://discord.com/api/webhooks/1/../secret", "secret-token")) {
            try {
                DiscordWebhook.validate(url);
                throw new AssertionError("Unsafe endpoint accepted: " + url);
            } catch (IllegalArgumentException expected) {
                check(!expected.getMessage().contains(url), "webhook secret not in error");
            }
        }
        String content = "@everyone\t\"quoted\"\nhello\\world";
        var payload = JsonParser.parseString(DiscordWebhook.payload(content)).getAsJsonObject();
        equal(content, payload.get("content").getAsString(), "JSON escaping");
        equal(0, payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").size(), "mentions disabled");
        equal(1900, JsonParser.parseString(DiscordWebhook.payload("a".repeat(3000))).getAsJsonObject()
                .get("content").getAsString().length(), "webhook content bounded");
    }

    private static void permissions() {
        CommandSender sender = stub(CommandSender.class, method -> method.equals("hasPermission"));
        SmartAdminCommand command = new SmartAdminCommand(null, null, null, null, null, null);
        check(command.onTabComplete(sender, null, "sa", new String[]{""}).contains("reset"), "admin completion");
        CommandSender denied = stub(CommandSender.class, method -> method.equals("hasPermission") ? false : null);
        equal(List.of(), command.onTabComplete(denied, null, "sa", new String[]{""}), "no permission completion");
        CommandSender noteOnly = (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> method.getName().equals("hasPermission") && "smartadmin.note".equals(args[0]));
        equal(List.of("help", "note", "version"), command.onTabComplete(noteOnly, null, "sa", new String[]{""}), "limited completion");
        equal(List.of(), command.onTabComplete(noteOnly, null, "sa", new String[]{"reset", ""}), "no target leak for denied command");
    }

    private static void exports(Path directory) throws Exception {
        Path one = EvidenceFiles.write(directory, "../Player", List.of("first"));
        Path two = EvidenceFiles.write(directory, "../Player", List.of("second"));
        check(!one.equals(two), "unique export files");
        equal(directory, one.getParent(), "export remains in folder");
        equal(List.of("first"), Files.readAllLines(one), "existing export preserved");
        equal("hello world", EvidenceFiles.plainText("&chello\nworld\u00a7r"), "plain note text");
    }

    private static void watchWorkflow() throws Exception {
        YamlConfiguration yaml = defaults();
        yaml.set("watch.enabled", false);
        SmartAdminConfig config = SmartAdminConfig.load(yaml);
        WatchService watch = new WatchService(() -> config);
        UUID staffId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        List<String> messages = new ArrayList<>();
        Player staff = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> staffId;
                    case "hasPermission" -> true;
                    case "sendMessage" -> { messages.add((String) args[0]); yield null; }
                    default -> null;
                });
        Player other = stub(Player.class, method -> method.equals("getUniqueId") ? otherId : null);
        equal(true, watch.toggle(staff, target), "watch enabled");
        watch.toggle(other, target);
        var snapshot = watch.watchedPlayers(staffId);
        try {
            snapshot.clear();
            throw new AssertionError("Watch snapshots must be immutable");
        } catch (UnsupportedOperationException expected) {
            equal(1, watch.watchedPlayers(staffId).size(), "immutable watch snapshot");
        }
        SmartAdminCommand commands = new SmartAdminCommand(null, null, null, watch, () -> config, null);
        commands.onCommand(staff, null, "sa", new String[]{"watch", "clear"});
        equal(0, watch.watchedPlayers(staffId).size(), "clear works when delivery disabled");
        equal(1, watch.watchedPlayers(otherId).size(), "clear preserves other staff watches");
        check(messages.stream().anyMatch(text -> text.contains("Cleared 1")), "clear confirms count");
        equal(1, snapshot.size(), "snapshot unaffected by subsequent clear");
        equal(0, watch.clear(staffId), "clear is idempotent");
        messages.clear();
        commands.onCommand(staff, null, "sa", new String[]{"watch", "list"});
        check(messages.stream().anyMatch(text -> text.contains("No players watched")), "empty list feedback");
        check(messages.stream().anyMatch(text -> text.contains("delivery disabled")), "disabled watch feedback");
        for (String page : List.of("0", "-1", "1001", "2147483647", "abc")) {
            messages.clear();
            commands.onCommand(staff, null, "sa", new String[]{"timeline", "Unknown", "10", page});
            check(messages.stream().anyMatch(text -> text.contains("Timeline page must")), "invalid page rejected before profile lookup");
        }
        watch.clear();
        equal(0, watch.watchedPlayers(otherId).size(), "shutdown clears all watches");
    }

    private static void timelinePages(Path directory) throws Exception {
        UUID player = UUID.randomUUID();
        try (SQLiteStorageService storage = new SQLiteStorageService(directory.resolve("pages.db").toFile())) {
            storage.initialize();
            storage.upsertPlayer(player, "Pages", 1000);
            equal(List.of(), storage.getTimelinePage(player, 10, 1), "empty timeline page");
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 35; i++) {
                ids.add(storage.addTimelineEvent(event(player, 1000, TimelineEventType.JOIN, 0, "Event " + i, "")).id());
            }
            storage.addTimelineEvent(event(UUID.randomUUID(), 2000, TimelineEventType.JOIN, 0, "Other player", ""));
            List<TimelineEvent> first = storage.getTimelinePage(player, 10, 1);
            equal(11, first.size(), "lookahead row included");
            equal(ids.get(34), first.getFirst().id(), "same timestamp tie uses newest id");
            equal(ids.get(24), storage.getTimelinePage(player, 10, 2).getFirst().id(), "second page starts after first ten");
            equal(5, storage.getTimelinePage(player, 10, 4).size(), "last page only remaining events");
            equal(List.of(), storage.getTimelinePage(player, 10, 5), "past end is empty");
            equal(List.of(), storage.getTimelinePage(UUID.randomUUID(), 10, 1), "pages isolate players");
            equal(storage.getRecentTimeline(player, 10), first.subList(0, 10), "existing recent behavior preserved");
            for (int[] input : List.of(new int[]{0, 1}, new int[]{31, 1}, new int[]{10, 0}, new int[]{10, Integer.MAX_VALUE})) {
                try {
                    storage.getTimelinePage(player, input[0], input[1]);
                    throw new AssertionError("Invalid pagination accepted");
                } catch (IllegalArgumentException expected) {
                    check(true, "pagination bounds enforced");
                }
            }
        }
    }

    private static void storageAndMining(Path directory) throws Exception {
        Path database = directory.resolve("test.db");
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        try (SQLiteStorageService storage = new SQLiteStorageService(database.toFile())) {
            storage.initialize();
            storage.upsertPlayer(uuid, "Investigation", now);
            storage.saveRiskAndEvent(40, event(uuid, now, TimelineEventType.STAFF_ACTION, 0, "Seed", ""));
            try {
                storage.saveRiskAndEvent(0, event(uuid, now, TimelineEventType.STAFF_ACTION, 0, null, ""));
                throw new AssertionError("Invalid timeline insert must fail");
            } catch (SQLException expected) {
                equal(40, storage.findProfile(uuid).orElseThrow().riskScore(), "risk rollback");
                equal(1, storage.getRecentTimeline(uuid, 30).size(), "timeline rollback");
            }
            storage.saveRiskAndEvent(0, event(uuid, now + 1, TimelineEventType.STAFF_ACTION, 0, "Reset", "oldScore=40"));
            equal(now, storage.findProfile(uuid).orElseThrow().lastSeen(), "offline reset preserves last seen");
            equal(0, storage.findProfileByName("INVESTIGATION").orElseThrow().riskScore(), "case insensitive lookup");
            storage.setAlertsEnabled(uuid, false);
            storage.addTimelineEvent(event(uuid, now - 10000, TimelineEventType.STAFF_NOTE, 0, "Note", "Older important note"));
            for (int i = 0; i < 31; i++) {
                storage.addTimelineEvent(event(uuid, now, TimelineEventType.JOIN, 0, "Join", ""));
            }
            equal(1, storage.getRecentNotes(uuid, 10).size(), "notes not hidden by newer timeline");
            storage.addTimelineEvent(event(uuid, now, TimelineEventType.MINE_VALUABLE_ORE, 1, "Ore", "material=DIAMOND_ORE_FAKE"));
            equal(0, storage.countTimelineEvents(uuid, "MINE_VALUABLE_ORE", "DIAMOND_ORE", now - 1), "exact material matching");
            storage.addTimelineEvent(event(uuid, now - 600001, TimelineEventType.ORE_BURST, 15, "Old burst", "oreGroup=diamond; count=10"));
            check(!storage.hasRecentSignal(uuid, "ORE_BURST", "oreGroup=diamond", now - 600000), "expired burst guard");

            YamlConfiguration yaml = defaults();
            yaml.set("alerts.enabled", false);
            yaml.set("mining.new-player.enabled", false);
            yaml.set("mining.burst-detection.diamond-threshold", 2);
            yaml.set("mining.burst-detection.ancient-debris-threshold", 2);
            SmartAdminConfig config = SmartAdminConfig.load(yaml);
            TimelineService timeline = new TimelineService(storage);
            WatchService watch = new WatchService(() -> config);
            RiskService risks = new RiskService(null, storage, timeline, new AlertService(null, storage, () -> config), watch, () -> config);
            MiningListener mining = new MiningListener(null, storage, risks, () -> config);
            Player player = stub(Player.class, method -> switch (method) {
                case "getUniqueId" -> uuid;
                case "getName" -> "Investigation";
                case "hasPermission" -> false;
                default -> null;
            });
            mine(mining, player, Material.DIAMOND_ORE);
            mine(mining, player, Material.DEEPSLATE_DIAMOND_ORE);
            equal(21, storage.findProfile(uuid).orElseThrow().riskScore(), "combined diamond burst");
            mine(mining, player, Material.IRON_ORE);
            equal(22, storage.findProfile(uuid).orElseThrow().riskScore(), "other ore cannot repeat diamond bonus");
            mine(mining, player, Material.DIAMOND_ORE);
            equal(25, storage.findProfile(uuid).orElseThrow().riskScore(), "burst cooldown");
            mine(mining, player, Material.ANCIENT_DEBRIS);
            mine(mining, player, Material.ANCIENT_DEBRIS);
            equal(50, storage.findProfile(uuid).orElseThrow().riskScore(), "independent debris bonus");
            equal(2, storage.miningCounts(uuid, now - 1).get("DIAMOND_ORE"), "grouped mining count");
            risks.addSignal(player, TimelineEventType.BLOCK_PLACE, null, Integer.MAX_VALUE, "Huge signal", "");
            equal(100, storage.findProfile(uuid).orElseThrow().riskScore(), "huge signal clamps safely");
            equal(50, storage.getRecentRiskSignals(uuid, 1).getFirst().riskChange(), "audit stores actual applied delta");
            risks.addSignal(player, TimelineEventType.BLOCK_PLACE, null, 5, "At cap", "");
            equal(0, storage.getRecentTimeline(uuid, 1).getFirst().riskChange(), "at-cap event records zero");
            UUID missing = UUID.randomUUID();
            try {
                storage.saveRiskAndEvent(0, event(missing, now, TimelineEventType.STAFF_ACTION, 0, "Reset", ""));
                throw new AssertionError("Unknown player reset must fail");
            } catch (SQLException expected) {
                equal(List.of(), storage.getRecentTimeline(missing, 10), "no orphan reset event");
            }
            UUID newUuid = UUID.randomUUID();
            Player newPlayer = stub(Player.class, method -> switch (method) {
                case "getUniqueId" -> newUuid;
                case "getName" -> "NewPlayer";
                case "hasPermission" -> false;
                default -> null;
            });
            yaml.set("mining.new-player.enabled", true);
            yaml.set("mining.new-player.valuable-ore-threshold", 2);
            SmartAdminConfig newConfig = SmartAdminConfig.load(yaml);
            RiskService newRisks = new RiskService(null, storage, timeline, new AlertService(null, storage, () -> newConfig), watch, () -> newConfig);
            MiningListener newMining = new MiningListener(null, storage, newRisks, () -> newConfig);
            mine(newMining, newPlayer, Material.IRON_ORE);
            mine(newMining, newPlayer, Material.IRON_ORE);
            mine(newMining, newPlayer, Material.IRON_ORE);
            equal(11, storage.findProfile(newUuid).orElseThrow().riskScore(), "new player bonus only once per window");
            Player bypassed = stub(Player.class, method -> switch (method) {
                case "getUniqueId" -> newUuid;
                case "getName" -> "NewPlayer";
                case "hasPermission" -> true;
                default -> null;
            });
            newRisks.addSignal(bypassed, TimelineEventType.BLOCK_PLACE, null, 50, "Bypassed", "");
            equal(11, storage.findProfile(newUuid).orElseThrow().riskScore(), "bypass preserves score");
            equal(0, storage.getRecentTimeline(newUuid, 1).getFirst().riskChange(), "bypass has zero delta");
            storage.decayRiskScores(2);
            equal(98, storage.findProfile(uuid).orElseThrow().riskScore(), "decay works");
        }
        try (SQLiteStorageService reopened = new SQLiteStorageService(database.toFile())) {
            reopened.initialize();
            equal(98, reopened.findProfile(uuid).orElseThrow().riskScore(), "risk persists after restart");
            equal(false, reopened.findProfile(uuid).orElseThrow().alertsEnabled(), "alert preference persists");
            check(reopened.hasRecentSignal(uuid, "ORE_BURST", "oreGroup=diamond", now - 600000), "burst guard persists");
        }
    }

    private static void mine(MiningListener mining, Player player, Material material) {
        Block block = stub(Block.class, method -> switch (method) {
            case "getType" -> material;
            case "getLocation" -> new Location(null, 12, -54, 30);
            default -> null;
        });
        mining.onBlockBreak(new BlockBreakEvent(block, player));
    }

    private static TimelineEvent event(UUID uuid, long timestamp, TimelineEventType type, int change, String reason, String details) {
        return new TimelineEvent(0, uuid, "Investigation", timestamp, type, null, null, null, null, change, reason, details);
    }

    private static YamlConfiguration defaults() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load("src/main/resources/config.yml");
        return yaml;
    }

    private static <T> T stub(Class<T> type, Function<String, Object> answers) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answers.apply(method.getName())));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        check(expected.equals(actual), message + ": expected " + expected + ", got " + actual);
    }
}
