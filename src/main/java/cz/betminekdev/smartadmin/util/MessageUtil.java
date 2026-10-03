package cz.betminekdev.smartadmin.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public static void send(CommandSender sender, String prefix, String message) {
        String safePrefix = prefix == null ? "" : prefix;
        String separator = safePrefix.isEmpty() || Character.isWhitespace(safePrefix.charAt(safePrefix.length() - 1)) ? "" : " ";
        sender.sendMessage(color(safePrefix + separator + message));
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
