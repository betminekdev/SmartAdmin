package cz.betminekdev.serverintel.util;

import org.bukkit.Location;

public final class LocationUtil {
    private LocationUtil() {
    }

    public static String compact(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown location";
        }
        return location.getWorld().getName()
                + " X:" + location.getBlockX()
                + " Y:" + location.getBlockY()
                + " Z:" + location.getBlockZ();
    }
}
