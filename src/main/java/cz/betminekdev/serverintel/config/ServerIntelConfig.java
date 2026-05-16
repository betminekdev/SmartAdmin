package cz.betminekdev.serverintel.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ServerIntelConfig {
    private final String prefix;
    private final int maxScore;
    private final boolean decayEnabled;
    private final int decayAmount;
    private final int decayIntervalMinutes;
    private final boolean scoreBypassedPlayers;
    private final boolean miningEnabled;
    private final Map<Material, Integer> valuableOres;
    private final boolean burstEnabled;
    private final int burstWindowMinutes;
    private final int diamondThreshold;
    private final int ancientDebrisThreshold;
    private final int burstExtraRisk;
    private final boolean newPlayerMiningEnabled;
    private final int newPlayerMaxPlaytimeMinutes;
    private final int newPlayerValuableOreThreshold;
    private final int newPlayerExtraRisk;
    private final boolean blockPlaceEnabled;
    private final int tntRisk;
    private final int lavaRisk;
    private final boolean chatEnabled;
    private final int spamMessageCount;
    private final int spamWindowSeconds;
    private final int spamRisk;
    private final int suspiciousLinkRisk;
    private final boolean alertsEnabled;
    private final int alertThreshold;
    private final int highRiskThreshold;
    private final int alertCooldownSeconds;
    private final String storageType;
    private final String databaseFile;
    private final int keepDataDays;
    private final boolean watchEnabled;
    private final boolean discordEnabled;
    private final String discordWebhookUrl;
    private final boolean discordHighRiskOnly;

    private ServerIntelConfig(FileConfiguration config) {
        this.prefix = config.getString("messages.prefix", "&8[&bServerIntel&8]&r ");
        this.maxScore = Math.max(1, config.getInt("risk.max-score", 100));
        this.decayEnabled = config.getBoolean("risk.decay-enabled", true);
        this.decayAmount = Math.max(0, config.getInt("risk.decay-amount", 2));
        this.decayIntervalMinutes = Math.max(1, config.getInt("risk.decay-interval-minutes", 30));
        this.scoreBypassedPlayers = config.getBoolean("risk.score-bypassed-players", false);
        this.miningEnabled = config.getBoolean("mining.enabled", true);
        this.valuableOres = loadValuableOres(config.getConfigurationSection("mining.valuable-ores"));
        this.burstEnabled = config.getBoolean("mining.burst-detection.enabled", true);
        this.burstWindowMinutes = Math.max(1, config.getInt("mining.burst-detection.time-window-minutes", 10));
        this.diamondThreshold = Math.max(1, config.getInt("mining.burst-detection.diamond-threshold", 10));
        this.ancientDebrisThreshold = Math.max(1, config.getInt("mining.burst-detection.ancient-debris-threshold", 6));
        this.burstExtraRisk = Math.max(0, config.getInt("mining.burst-detection.extra-risk", 15));
        this.newPlayerMiningEnabled = config.getBoolean("mining.new-player.enabled", true);
        this.newPlayerMaxPlaytimeMinutes = Math.max(1, config.getInt("mining.new-player.max-playtime-minutes", 60));
        this.newPlayerValuableOreThreshold = Math.max(1, config.getInt("mining.new-player.valuable-ore-threshold", 6));
        this.newPlayerExtraRisk = Math.max(0, config.getInt("mining.new-player.extra-risk", 8));
        this.blockPlaceEnabled = config.getBoolean("signals.block-place.enabled", true);
        this.tntRisk = Math.max(0, config.getInt("signals.block-place.tnt-risk", 4));
        this.lavaRisk = Math.max(0, config.getInt("signals.block-place.lava-risk", 3));
        this.chatEnabled = config.getBoolean("signals.chat.enabled", true);
        this.spamMessageCount = Math.max(2, config.getInt("signals.chat.spam-message-count", 5));
        this.spamWindowSeconds = Math.max(1, config.getInt("signals.chat.spam-window-seconds", 8));
        this.spamRisk = Math.max(0, config.getInt("signals.chat.spam-risk", 5));
        this.suspiciousLinkRisk = Math.max(0, config.getInt("signals.chat.suspicious-link-risk", 4));
        this.alertsEnabled = config.getBoolean("alerts.enabled", true);
        this.alertThreshold = Math.max(1, config.getInt("alerts.threshold", 60));
        this.highRiskThreshold = Math.max(alertThreshold, config.getInt("alerts.high-risk-threshold", 80));
        this.alertCooldownSeconds = Math.max(1, config.getInt("alerts.cooldown-seconds", 30));
        this.storageType = config.getString("storage.type", "sqlite");
        this.databaseFile = config.getString("storage.database-file", "plugins/ServerIntel/serverintel.db");
        this.keepDataDays = Math.max(1, config.getInt("storage.keep-data-days", 14));
        this.watchEnabled = config.getBoolean("watch.enabled", true);
        this.discordEnabled = config.getBoolean("discord.enabled", false);
        this.discordWebhookUrl = config.getString("discord.webhook-url", "");
        this.discordHighRiskOnly = config.getBoolean("discord.high-risk-only", true);
    }

    public static ServerIntelConfig load(FileConfiguration config) {
        return new ServerIntelConfig(config);
    }

    private static Map<Material, Integer> loadValuableOres(ConfigurationSection section) {
        EnumMap<Material, Integer> result = new EnumMap<>(Material.class);
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            if (material != null) {
                result.put(material, Math.max(0, section.getInt(key)));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public String prefix() {
        return prefix;
    }

    public int maxScore() {
        return maxScore;
    }

    public boolean decayEnabled() {
        return decayEnabled;
    }

    public int decayAmount() {
        return decayAmount;
    }

    public int decayIntervalMinutes() {
        return decayIntervalMinutes;
    }

    public boolean scoreBypassedPlayers() {
        return scoreBypassedPlayers;
    }

    public boolean miningEnabled() {
        return miningEnabled;
    }

    public Map<Material, Integer> valuableOres() {
        return valuableOres;
    }

    public boolean burstEnabled() {
        return burstEnabled;
    }

    public int burstWindowMinutes() {
        return burstWindowMinutes;
    }

    public int diamondThreshold() {
        return diamondThreshold;
    }

    public int ancientDebrisThreshold() {
        return ancientDebrisThreshold;
    }

    public int burstExtraRisk() {
        return burstExtraRisk;
    }

    public boolean newPlayerMiningEnabled() {
        return newPlayerMiningEnabled;
    }

    public int newPlayerMaxPlaytimeMinutes() {
        return newPlayerMaxPlaytimeMinutes;
    }

    public int newPlayerValuableOreThreshold() {
        return newPlayerValuableOreThreshold;
    }

    public int newPlayerExtraRisk() {
        return newPlayerExtraRisk;
    }

    public boolean blockPlaceEnabled() {
        return blockPlaceEnabled;
    }

    public int tntRisk() {
        return tntRisk;
    }

    public int lavaRisk() {
        return lavaRisk;
    }

    public boolean chatEnabled() {
        return chatEnabled;
    }

    public int spamMessageCount() {
        return spamMessageCount;
    }

    public int spamWindowSeconds() {
        return spamWindowSeconds;
    }

    public int spamRisk() {
        return spamRisk;
    }

    public int suspiciousLinkRisk() {
        return suspiciousLinkRisk;
    }

    public boolean alertsEnabled() {
        return alertsEnabled;
    }

    public int alertThreshold() {
        return alertThreshold;
    }

    public int highRiskThreshold() {
        return highRiskThreshold;
    }

    public int alertCooldownSeconds() {
        return alertCooldownSeconds;
    }

    public String storageType() {
        return storageType;
    }

    public String databaseFile() {
        return databaseFile;
    }

    public int keepDataDays() {
        return keepDataDays;
    }

    public boolean watchEnabled() {
        return watchEnabled;
    }

    public boolean discordEnabled() {
        return discordEnabled;
    }

    public String discordWebhookUrl() {
        return discordWebhookUrl;
    }

    public boolean discordHighRiskOnly() {
        return discordHighRiskOnly;
    }
}
