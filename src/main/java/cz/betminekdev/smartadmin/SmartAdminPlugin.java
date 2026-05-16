package cz.betminekdev.smartadmin;

import cz.betminekdev.smartadmin.alerts.AlertService;
import cz.betminekdev.smartadmin.commands.SmartAdminCommand;
import cz.betminekdev.smartadmin.config.SmartAdminConfig;
import cz.betminekdev.smartadmin.listeners.BlockPlaceListener;
import cz.betminekdev.smartadmin.listeners.ChatListener;
import cz.betminekdev.smartadmin.listeners.MiningListener;
import cz.betminekdev.smartadmin.listeners.PlayerJoinListener;
import cz.betminekdev.smartadmin.risk.RiskService;
import cz.betminekdev.smartadmin.storage.SQLiteStorageService;
import cz.betminekdev.smartadmin.storage.StorageService;
import cz.betminekdev.smartadmin.timeline.TimelineService;
import cz.betminekdev.smartadmin.watch.WatchService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.SQLException;
import java.util.Locale;

public final class SmartAdminPlugin extends JavaPlugin {
    private SmartAdminConfig smartAdminConfig;
    private StorageService storageService;
    private TimelineService timelineService;
    private WatchService watchService;
    private AlertService alertService;
    private RiskService riskService;
    private BukkitTask decayTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSmartAdminConfig();

        if (!"sqlite".equals(smartAdminConfig.storageType().toLowerCase(Locale.ROOT))) {
            getLogger().warning("Only SQLite storage is supported in v0.1. Falling back to SQLite.");
        }

        storageService = new SQLiteStorageService(new File(smartAdminConfig.databaseFile()));
        try {
            storageService.initialize();
            storageService.purgeOldTimelineEvents(smartAdminConfig.keepDataDays());
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

        getLogger().info("SmartAdmin " + getDescription().getVersion() + " enabled. Command root: /smartadmin, aliases: /sa and /si.");
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
        getLogger().info("SmartAdmin disabled.");
    }

    public SmartAdminConfig config() {
        return smartAdminConfig;
    }

    private void loadSmartAdminConfig() {
        reloadConfig();
        smartAdminConfig = SmartAdminConfig.load(getConfig());
    }

    private void reloadSmartAdmin() {
        loadSmartAdminConfig();
        restartDecayTask();
        getLogger().info("SmartAdmin configuration reloaded.");
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

        SmartAdminCommand executor = new SmartAdminCommand(this, storageService, timelineService, watchService, this::config, this::reloadSmartAdmin);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void restartDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
        if (!smartAdminConfig.decayEnabled() || smartAdminConfig.decayAmount() <= 0) {
            return;
        }

        long intervalTicks = smartAdminConfig.decayIntervalMinutes() * 60L * 20L;
        decayTask = getServer().getScheduler().runTaskTimer(this, riskService::decayScores, intervalTicks, intervalTicks);
    }
}
