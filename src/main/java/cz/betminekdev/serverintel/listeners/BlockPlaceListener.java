package cz.betminekdev.serverintel.listeners;

import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.risk.RiskService;
import cz.betminekdev.serverintel.timeline.TimelineEventType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.Supplier;

public final class BlockPlaceListener implements Listener {
    private final RiskService riskService;
    private final Supplier<ServerIntelConfig> config;

    public BlockPlaceListener(RiskService riskService, Supplier<ServerIntelConfig> config) {
        this.riskService = riskService;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ServerIntelConfig current = config.get();
        if (!current.blockPlaceEnabled()) {
            return;
        }

        Material material = event.getBlockPlaced().getType();
        if (material == Material.TNT) {
            riskService.addSignal(event.getPlayer(), TimelineEventType.BLOCK_PLACE, event.getBlockPlaced().getLocation(),
                    current.tntRisk(), "Placed TNT", "material=TNT");
        } else if (material == Material.LAVA) {
            riskService.addSignal(event.getPlayer(), TimelineEventType.BLOCK_PLACE, event.getBlockPlaced().getLocation(),
                    current.lavaRisk(), "Placed lava", "material=LAVA");
        }
    }
}
