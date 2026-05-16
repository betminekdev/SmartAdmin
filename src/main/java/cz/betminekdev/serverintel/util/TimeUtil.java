package cz.betminekdev.serverintel.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private TimeUtil() {
    }

    public static String dateTime(long millis) {
        return DATE_TIME.format(Instant.ofEpochMilli(millis));
    }

    public static String time(long millis) {
        return TIME.format(Instant.ofEpochMilli(millis));
    }
}
