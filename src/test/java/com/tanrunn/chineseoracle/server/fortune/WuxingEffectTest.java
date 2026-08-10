package com.tanrunn.chineseoracle.server.fortune;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** WuxingEffect tests (pure overloads). */
class WuxingEffectTest {

    @Test
    void nullOrDisabledYieldsNeutral() {
        assertEquals(1f, WuxingEffect.bonusMul(null, 2f));
        assertEquals(1f, WuxingEffect.bonusMul("金", 0f));
        assertEquals(0f, WuxingEffect.fishingLuckBonus(null, 2f));
        assertEquals(0f, WuxingEffect.fishingLuckBonus("水", 0f));
    }

    @Test
    void enabledYieldsTinyBonus() {
        assertEquals(1.02f, WuxingEffect.bonusMul("金", 2f), 0.0001f);
        assertEquals(1.03f, WuxingEffect.bonusMul("木", 3f), 0.0001f);
        assertEquals(0.2f, WuxingEffect.fishingLuckBonus("水", 2f), 0.0001f);
    }
}
