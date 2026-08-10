package com.tanrunn.chineseoracle.server.fortune;

/**
 * 二十四节气 (design document section 3.5, v2): computed day-by-day from the
 * sun's apparent ecliptic longitude (solar terms occur at every 15°), so the
 * spacing reflects the non-circular orbit instead of a fixed table. The game
 * year starts at 立春 (λ = 315°).
 */
public final class SolarTerm {
    private static final String[] TERMS = {
            "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
            "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
            "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"
    };

    private SolarTerm() {
    }

    public static String forDayIndex(long dayIndex) {
        double dayOfYear = Math.floorMod(dayIndex, 365L);
        double meanLongitude = 315.0 + 0.9856474 * dayOfYear;
        double anomaly = Math.toRadians(360.0 * (dayOfYear - 336.0) / 365.0);
        double lambda = meanLongitude + 1.915 * Math.sin(anomaly) + 0.020 * Math.sin(2 * anomaly);
        double adjusted = lambda;
        while (adjusted < 315.0) {
            adjusted += 360.0;
        }
        int index = (int) ((adjusted - 315.0) / 15.0) % 24;
        return TERMS[index];
    }
}
