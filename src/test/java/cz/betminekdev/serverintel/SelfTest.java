package cz.betminekdev.serverintel;

import cz.betminekdev.serverintel.risk.RiskLevel;
import cz.betminekdev.serverintel.risk.RiskService;

public final class SelfTest {
    private SelfTest() {
    }

    public static void main(String[] args) {
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(0), "0 should be SAFE");
        assertEquals(RiskLevel.SAFE, RiskLevel.fromScore(25), "25 should be SAFE");
        assertEquals(RiskLevel.WATCH, RiskLevel.fromScore(26), "26 should be WATCH");
        assertEquals(RiskLevel.WATCH, RiskLevel.fromScore(50), "50 should be WATCH");
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(51), "51 should be SUSPICIOUS");
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(75), "75 should be SUSPICIOUS");
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(76), "76 should be HIGH_RISK");
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(100), "100 should be HIGH_RISK");

        assertEquals(0, RiskService.clampScore(-20, 100), "negative scores should clamp to zero");
        assertEquals(64, RiskService.clampScore(64, 100), "normal scores should not change");
        assertEquals(100, RiskService.clampScore(150, 100), "scores should clamp to max");
        assertEquals(1, RiskService.clampScore(10, 0), "invalid max score should behave as one");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected=" + expected + " actual=" + actual);
        }
    }
}
