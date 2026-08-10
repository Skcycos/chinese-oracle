package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.Config;
import net.minecraft.server.MinecraftServer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 十二时辰 (design document section 3.5, v1.1): derived from game day time or the
 * wall clock. A few "吉" shichen give a tiny event-driven bonus; computed only in
 * hooks, never per tick.
 */
public final class Shichen {
    private static final String[] NAMES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    // 卯(3) 午(6) 酉(9) are the turning points of the day.
    private static final boolean[] AUSPICIOUS = {false, false, false, true, false, false, true, false, false, true, false, false};

    private Shichen() {
    }

    public static int currentIndex(MinecraftServer server) {
        int hour = switch (Config.DAY_SOURCE.get()) {
            case "wall_clock_utc" -> LocalDateTime.now(ZoneOffset.UTC).getHour();
            case "wall_clock_offset" -> LocalDateTime.now(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(Config.WALL_CLOCK_OFFSET_HOURS.get()))).getHour();
            default -> (6 + (int) (server.overworld().getDayTime() / 1000)) % 24;
        };
        return ((hour + 1) / 2) % 12;
    }

    public static String name(MinecraftServer server) {
        return NAMES[currentIndex(server)] + "时";
    }

    public static boolean isAuspicious(MinecraftServer server) {
        return AUSPICIOUS[currentIndex(server)];
    }

    public static float miningBonusMul(MinecraftServer server) {
        if (!Config.SHICHEN_BONUS.get() || !isAuspicious(server)) return 1f;
        return 1f + Config.SHICHEN_BONUS_PERCENT.get().floatValue() / 100f;
    }

    public static float fishingLuckBonus(MinecraftServer server) {
        if (!Config.SHICHEN_BONUS.get() || !isAuspicious(server)) return 0f;
        return Config.SHICHEN_BONUS_PERCENT.get().floatValue() / 100f * 10f;
    }
}
