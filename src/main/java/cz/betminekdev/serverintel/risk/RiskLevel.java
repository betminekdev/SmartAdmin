package cz.betminekdev.serverintel.risk;

public enum RiskLevel {
    SAFE("&a"),
    WATCH("&e"),
    SUSPICIOUS("&6"),
    HIGH_RISK("&c");

    private final String color;

    RiskLevel(String color) {
        this.color = color;
    }

    public String color() {
        return color;
    }

    public static RiskLevel fromScore(int score) {
        if (score <= 25) {
            return SAFE;
        }
        if (score <= 50) {
            return WATCH;
        }
        if (score <= 75) {
            return SUSPICIOUS;
        }
        return HIGH_RISK;
    }
}
