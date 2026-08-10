package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.Config;

/**
 * 五行极轻全局乘区 (design document section 3.5, v1): the drawn element grants
 * a tiny flat bonus on hooked actions. Pure overloads take the percent
 * explicitly for unit tests; hooks read it from the config.
 */
public final class WuxingEffect {
    private WuxingEffect() {
    }

    public static float bonusMul(String wuxing) {
        return bonusMul(wuxing, Config.WUXING_EFFECT_ENABLED.get() ? Config.WUXING_EFFECT_PERCENT.get().floatValue() : 0f);
    }

    public static float bonusMul(String wuxing, float percent) {
        if (wuxing == null || percent <= 0f) return 1f;
        return 1f + percent / 100f;
    }

    public static float fishingLuckBonus(String wuxing) {
        return fishingLuckBonus(wuxing, Config.WUXING_EFFECT_ENABLED.get() ? Config.WUXING_EFFECT_PERCENT.get().floatValue() : 0f);
    }

    public static float fishingLuckBonus(String wuxing, float percent) {
        if (wuxing == null || percent <= 0f) return 0f;
        return percent / 100f * 10f;
    }
}
