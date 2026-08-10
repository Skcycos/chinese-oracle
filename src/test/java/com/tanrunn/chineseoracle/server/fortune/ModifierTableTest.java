package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.TestRegistries;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.common.Modifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** ModifierTable tests (design document section 8.8). */
class ModifierTableTest {

    @Test
    void neutralSnapshotYieldsNeutralModifier() {
        FortuneSnapshot s = new FortuneSnapshot(1, "ping", List.of(), List.of(), null, null, 0, 0);
        Modifier m = ModifierTable.build(s, TestRegistries.smallRegistry(), 0.15f);
        assertEquals(Modifier.neutral(), m);
    }

    @Test
    void yiMiningBoostsSpeed() {
        // ss_da_ji scale 1.3, kaikuang mining_speed 0.10 -> mul = 1 + 0.10 * 1.3 = 1.13
        FortuneSnapshot s = new FortuneSnapshot(1, "ss_da_ji", List.of("kaikuang"), List.of(), "s_good", "金", 0, 0);
        Modifier m = ModifierTable.build(s, TestRegistries.smallRegistry(), 0.15f);
        assertEquals(1.13f, m.miningSpeedMul(), 0.001f);
    }

    @Test
    void jiFlipsEffectToNegative() {
        // ji entry flips: mul = 1 - 0.10 * 1.3 = 0.87
        FortuneSnapshot s = new FortuneSnapshot(1, "ss_da_ji", List.of(), List.of("dongtu"), "s_good", null, 0, 0);
        Modifier m = ModifierTable.build(s, TestRegistries.smallRegistry(), 0.15f);
        assertEquals(0.87f, m.miningSpeedMul(), 0.001f);
    }

    @Test
    void capClampsMultipliers() {
        // uncapped would be 1.13, cap 0.05 clamps to 1.05
        FortuneSnapshot s = new FortuneSnapshot(1, "ss_da_ji", List.of("kaikuang"), List.of(), "s_good", null, 0, 0);
        Modifier m = ModifierTable.build(s, TestRegistries.smallRegistry(), 0.05f);
        assertEquals(1.05f, m.miningSpeedMul(), 0.001f);
    }

    @Test
    void weakerTierScalesDown() {
        // xiao_ji scale 0.6 -> mul = 1 + 0.10 * 0.6 = 1.06
        FortuneSnapshot s = new FortuneSnapshot(1, "xiao_ji", List.of("kaikuang"), List.of(), "s_mid", null, 0, 0);
        Modifier m = ModifierTable.build(s, TestRegistries.smallRegistry(), 0.15f);
        assertEquals(1.06f, m.miningSpeedMul(), 0.001f);
    }

    @Test
    void restHealIsCollected() {
        // custom registry with an 安眠-style rest entry
        var registry = new com.tanrunn.chineseoracle.server.registry.FortuneRegistry(
                Map.of("ping", new com.tanrunn.chineseoracle.server.registry.TierData("ping", "平", 4, 10, 0.3f, true)),
                Map.of("anmian", new com.tanrunn.chineseoracle.server.registry.YiJiEntry("anmian", "yi", "安眠", Map.of("rest_heal", 2.0f), 1)),
                Map.of());
        FortuneSnapshot s = new FortuneSnapshot(1, "ping", List.of("anmian"), List.of(), null, null, 0, 0);
        Modifier m = ModifierTable.build(s, registry, 0.15f);
        assertEquals(0.6f, m.restHeal(), 0.001f); // 2.0 * 0.3 (ping scale)
        assertEquals(Modifier.neutral().restHeal(), Modifier.neutral().restHeal());
    }
}
