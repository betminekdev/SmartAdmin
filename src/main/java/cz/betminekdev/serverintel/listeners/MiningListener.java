package cz.betminekdev.serverintel.listeners;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.risk.RiskService;
import cz.betminekdev.serverintel.storage.PlayerProfile;
import cz.betminekdev.serverintel.storage.StorageService;
import cz.betminekdev.serverintel.timeline.TimelineEventType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
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
    private final Supplier<ServerIntelConfig> config;

    public MiningListener(JavaPlugin plugin, StorageService storage, RiskService riskService, Supplier<ServerIntelConfig> config) {
        this.plugin = plugin;
        this.storage = storage;
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ServerIntelConfig current = config.get();
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

    private void runBurstChecks(BlockBreakEvent event, ServerIntelConfig current) {
        if (!current.burstEnabled() && !current.newPlayerMiningEnabled()) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        long since = System.currentTimeMillis() - current.burstWindowMinutes() * 60_000L;
        try {
            if (current.burstEnabled()) {
                int diamondCount = count(uuid, Material.DIAMOND_ORE, since) + count(uuid, Material.DEEPSLATE_DIAMOND_ORE, since);
                if (diamondCount == current.diamondThreshold()) {
                    riskService.addSignal(
                            event.getPlayer(),
                            TimelineEventType.ORE_BURST,
                            event.getBlock().getLocation(),
                            current.burstExtraRisk(),
                            "High ore burst detected: " + diamondCount + " diamond ores in " + current.burstWindowMinutes() + " minutes",
                            "oreGroup=diamond; count=" + diamondCount + "; windowMinutes=" + current.burstWindowMinutes()
                    );
                }

                int ancientDebrisCount = count(uuid, Material.ANCIENT_DEBRIS, since);
                if (ancientDebrisCount == current.ancientDebrisThreshold()) {
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
                int valuableCount = countValuableOres(uuid, current.valuableOres(), since);
                if (valuableCount == current.newPlayerValuableOreThreshold()) {
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

    private int countValuableOres(UUID uuid, Map<Material, Integer> ores, long since) throws SQLException {
        int total = 0;
        for (Material material : ores.keySet()) {
            total += count(uuid, material, since);
        }
        return total;
    }

    private int count(UUID uuid, Material material, long since) throws SQLException {
        return storage.countTimelineEvents(uuid, TimelineEventType.MINE_VALUABLE_ORE.name(), material.name(), since);
    }

    private boolean isNewPlayer(UUID uuid, ServerIntelConfig current) throws SQLException {
        Optional<PlayerProfile> profile = storage.findProfile(uuid);
        if (profile.isEmpty()) {
            return true;
        }
        long maxAge = current.newPlayerMaxPlaytimeMinutes() * 60_000L;
        return System.currentTimeMillis() - profile.get().firstSeen() <= maxAge;
    }
}
