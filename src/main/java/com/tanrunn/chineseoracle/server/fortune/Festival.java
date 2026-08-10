package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.Config;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;

/**
 * 节日事件 (design document section 3.5, v2). Festival definitions are fully
 * datapack-driven (data/chinese_oracle/festivals/*.json) so operators can add
 * festivals without shipping a new jar. This class only computes the bonus.
 */
public final class Festival {
    private Festival() {
    }

    public static String current(long dayIndex) {
        return FortuneRegistry.get().currentFestival(dayIndex);
    }

    public static float bonusMul(long dayIndex) {
        if (current(dayIndex) == null || !Config.FESTIVAL_BONUS.get()) return 1f;
        return 1f + Config.FESTIVAL_BONUS_PERCENT.get().floatValue() / 100f;
    }

    public static float fishingLuckBonus(long dayIndex) {
        if (current(dayIndex) == null || !Config.FESTIVAL_BONUS.get()) return 0f;
        return Config.FESTIVAL_BONUS_PERCENT.get().floatValue() / 100f * 10f;
    }
}
