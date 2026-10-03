package cz.betminekdev.smartadmin.listeners;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.risk.RiskService;
import cz.betminekdev.smartadmin.storage.PlayerProfile;
import cz.betminekdev.smartadmin.storage.StorageService;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class MiningListener implements Listener {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final RiskService riskService;
    private final Supplier<SmartAdminConfig> config;

    public MiningListener(JavaPlugin plugin, StorageService storage, RiskService riskService, Supplier<SmartAdminConfig> config) {
        this.plugin = plugin;
        this.storage = storage;
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        SmartAdminConfig current = config.get();
        if (!current.miningEnabled()) {
            return;
        }

        Material material = event.getBlock().getType();
        Integer risk = current.valuableOres().get(material);
        if (risk == null) {
            return;
        }

        String materialName = material.name();
        riskService.addSignal(
                event.getPlayer(),
                TimelineEventType.MINE_VALUABLE_ORE,
                event.getBlock().getLocation(),
                risk,
                "Broke " + materialName,
                "material=" + materialName
        );

        runBurstChecks(event, current);
    }

    private void runBurstChecks(BlockBreakEvent event, SmartAdminConfig current) {
        if (!current.burstEnabled() && !current.newPlayerMiningEnabled()) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        long since = System.currentTimeMillis() - current.burstWindowMinutes() * 60_000L;
        try {
            Map<String, Integer> counts = storage.miningCounts(uuid, since);
            Material mined = event.getBlock().getType();
            if (current.burstEnabled()) {
                int diamondCount = counts.getOrDefault("DIAMOND_ORE", 0) + counts.getOrDefault("DEEPSLATE_DIAMOND_ORE", 0);
                if ((mined == Material.DIAMOND_ORE || mined == Material.DEEPSLATE_DIAMOND_ORE)
                        && diamondCount >= current.diamondThreshold()
                        && !storage.hasRecentSignal(uuid, TimelineEventType.ORE_BURST.name(), "oreGroup=diamond", since)) {
                    riskService.addSignal(
                            event.getPlayer(),
                            TimelineEventType.ORE_BURST,
                            event.getBlock().getLocation(),
                            current.burstExtraRisk(),
                            "High ore burst detected: " + diamondCount + " diamond ores in " + current.burstWindowMinutes() + " minutes",
                            "oreGroup=diamond; count=" + diamondCount + "; windowMinutes=" + current.burstWindowMinutes()
                    );
                }

                int ancientDebrisCount = counts.getOrDefault("ANCIENT_DEBRIS", 0);
                if (mined == Material.ANCIENT_DEBRIS && ancientDebrisCount >= current.ancientDebrisThreshold()
                        && !storage.hasRecentSignal(uuid, TimelineEventType.ORE_BURST.name(), "oreGroup=ancient_debris", since)) {
                    riskService.addSignal(
                            event.getPlayer(),
                            TimelineEventType.ORE_BURST,
                            event.getBlock().getLocation(),
                            current.burstExtraRisk(),
                            "Ancient debris burst detected: " + ancientDebrisCount + " ancient debris in " + current.burstWindowMinutes() + " minutes",
                            "oreGroup=ancient_debris; count=" + ancientDebrisCount + "; windowMinutes=" + current.burstWindowMinutes()
                    );
                }
            }

            if (current.newPlayerMiningEnabled() && isNewPlayer(uuid, current)) {
                int valuableCount = current.valuableOres().keySet().stream()
                        .mapToInt(material -> counts.getOrDefault(material.name(), 0)).sum();
                if (valuableCount >= current.newPlayerValuableOreThreshold()
                        && !storage.hasRecentSignal(uuid, TimelineEventType.NEW_PLAYER_MINING.name(), "", since)) {
                    riskService.addSignal(
                            event.getPlayer(),
                            TimelineEventType.NEW_PLAYER_MINING,
                            event.getBlock().getLocation(),
                            current.newPlayerExtraRisk(),
                            "New player with unusual mining activity",
                            "valuableOres=" + valuableCount + "; windowMinutes=" + current.burstWindowMinutes()
                    );
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not evaluate mining burst for " + event.getPlayer().getName() + ": " + exception.getMessage());
        }
    }

    private boolean isNewPlayer(UUID uuid, SmartAdminConfig current) throws SQLException {
        Optional<PlayerProfile> profile = storage.findProfile(uuid);
        if (profile.isEmpty()) {
            return true;
        }
        long maxAge = current.newPlayerMaxPlaytimeMinutes() * 60_000L;
        return System.currentTimeMillis() - profile.get().firstSeen() <= maxAge;
    }
}
