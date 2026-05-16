package cz.betminekdev.serverintel.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public static void send(CommandSender sender, String prefix, String message) {
        sender.sendMessage(color(prefix + message));
    }

    public static String riskColor(int score) {
        if (score <= 25) {
            return "&a";
        }
        if (score <= 50) {
            return "&e";
        }
        if (score <= 75) {
            return "&6";
        }
        return "&c";
    }
}
