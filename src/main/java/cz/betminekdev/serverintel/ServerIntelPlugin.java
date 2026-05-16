package cz.betminekdev.serverintel;

import cz.betminekdev.serverintel.alerts.AlertService;
import cz.betminekdev.serverintel.commands.ServerIntelCommand;
import cz.betminekdev.serverintel.config.ServerIntelConfig;
import cz.betminekdev.serverintel.listeners.BlockPlaceListener;
import cz.betminekdev.serverintel.listeners.ChatListener;
import cz.betminekdev.serverintel.listeners.MiningListener;
import cz.betminekdev.serverintel.listeners.PlayerJoinListener;
import cz.betminekdev.serverintel.risk.RiskService;
import cz.betminekdev.serverintel.storage.SQLiteStorageService;
import cz.betminekdev.serverintel.storage.StorageService;
import cz.betminekdev.serverintel.timeline.TimelineService;
import cz.betminekdev.serverintel.watch.WatchService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.SQLException;
import java.util.Locale;

public final class ServerIntelPlugin extends JavaPlugin {
    private ServerIntelConfig serverIntelConfig;
    private StorageService storageService;
    private TimelineService timelineService;
    private WatchService watchService;
    private AlertService alertService;
    private RiskService riskService;
    private BukkitTask decayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadServerIntelConfig();

        if (!"sqlite".equals(serverIntelConfig.storageType().toLowerCase(Locale.ROOT))) {
            getLogger().warning("Only SQLite storage is supported in v0.1. Falling back to SQLite.");
        }

        storageService = new SQLiteStorageService(new File(serverIntelConfig.databaseFile()));
        try {
            storageService.initialize();
            storageService.purgeOldTimelineEvents(serverIntelConfig.keepDataDays());
        } catch (SQLException exception) {
            getLogger().severe("Could not initialize SQLite storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        timelineService = new TimelineService(storageService);
        watchService = new WatchService(this::config);
        alertService = new AlertService(this, storageService, this::config);
        riskService = new RiskService(this, storageService, timelineService, alertService, watchService, this::config);

        registerListeners();
        registerCommands();
        restartDecayTask();

        getLogger().info("ServerIntel " + getDescription().getVersion() + " enabled. Command root: /smartadmin and /sa.");
    }

    @Override
    public void onDisable() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
        if (watchService != null) {
            watchService.clear();
        }
        if (alertService != null) {
            alertService.clearCooldowns();
        }
        if (storageService != null) {
            storageService.close();
            storageService = null;
        }
        getLogger().info("ServerIntel disabled.");
    }

    public ServerIntelConfig config() {
        return serverIntelConfig;
    }

    private void loadServerIntelConfig() {
        reloadConfig();
        serverIntelConfig = ServerIntelConfig.load(getConfig());
    }

    private void reloadServerIntel() {
        loadServerIntelConfig();
        restartDecayTask();
        getLogger().info("ServerIntel configuration reloaded.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, storageService, timelineService), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this, storageService, riskService, this::config), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(riskService, this::config), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, riskService, this::config), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("smartadmin");
        if (command == null) {
            getLogger().severe("Command smartadmin is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ServerIntelCommand executor = new ServerIntelCommand(this, storageService, timelineService, watchService, this::config, this::reloadServerIntel);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void restartDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
        if (!serverIntelConfig.decayEnabled() || serverIntelConfig.decayAmount() <= 0) {
            return;
        }
        long intervalTicks = serverIntelConfig.decayIntervalMinutes() * 60L * 20L;
        decayTask = getServer().getScheduler().runTaskTimer(this, riskService::decayScores, intervalTicks, intervalTicks);
    }
}
