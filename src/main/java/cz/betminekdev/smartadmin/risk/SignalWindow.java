package cz.betminekdev.smartadmin.risk;

import java.util.ArrayDeque;

/** Bounded sliding window, owned by the server thread. */
public final class SignalWindow {
    private final ArrayDeque<Long> events = new ArrayDeque<>();
    private long lastSignal = Long.MIN_VALUE;

    public boolean record(long now, long windowMillis, int threshold) {
        int limit = Math.max(2, threshold);
        while (!events.isEmpty() && events.peekFirst() <= now - windowMillis) {
            events.removeFirst();
        }
        events.addLast(now);
        while (events.size() > limit) {
            events.removeFirst();
        }
        if (events.size() >= limit && (lastSignal == Long.MIN_VALUE || now - lastSignal >= windowMillis)) {
            lastSignal = now;
            return true;
        }
        return false;
    }
}
