package cz.betminekdev.smartadmin.listeners;

import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.risk.RiskService;
import cz.betminekdev.smartadmin.timeline.TimelineEventType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.function.Supplier;

public final class BlockPlaceListener implements Listener {
    private final RiskService riskService;
    private final Supplier<SmartAdminConfig> config;

    public BlockPlaceListener(RiskService riskService, Supplier<SmartAdminConfig> config) {
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        SmartAdminConfig current = config.get();
        if (!current.blockPlaceEnabled()) {
            return;
        }

        Material material = event.getBlockPlaced().getType();
        if (material == Material.TNT) {
            riskService.addSignal(event.getPlayer(), TimelineEventType.BLOCK_PLACE, event.getBlockPlaced().getLocation(),
                    current.tntRisk(), "Placed TNT", "material=TNT");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        SmartAdminConfig current = config.get();
        if (current.blockPlaceEnabled() && event.getBucket() == Material.LAVA_BUCKET) {
            riskService.addSignal(event.getPlayer(), TimelineEventType.BLOCK_PLACE, event.getBlock().getLocation(),
                    current.lavaRisk(), "Emptied lava bucket", "material=LAVA");
        }
    }
}
