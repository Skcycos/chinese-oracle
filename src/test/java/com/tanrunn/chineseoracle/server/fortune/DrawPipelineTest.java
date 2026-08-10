package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.TestRegistries;
import com.tanrunn.chineseoracle.common.ActionType;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.SignEntry;
import com.tanrunn.chineseoracle.server.registry.TierData;
import com.tanrunn.chineseoracle.server.registry.YiJiEntry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DrawPipeline tests (design document section 8.8). */
class DrawPipelineTest {

    private static final int MAX_YI = 3;
    private static final int MAX_JI = 2;
    private static final int SIGN_PREFER = 3;

    @Test
    void sameSeedIsDeterministic() {
        FortuneSnapshot a = DrawPipeline.draw(100, 0x1234L, null, TestRegistries.smallRegistry(), MAX_YI, MAX_JI, SIGN_PREFER);
        FortuneSnapshot b = DrawPipeline.draw(100, 0x1234L, null, TestRegistries.smallRegistry(), MAX_YI, MAX_JI, SIGN_PREFER);
        assertEquals(a, b);
    }

    @Test
    void drawnTierExistsAndSignRangeMatchesTier() {
        FortuneRegistry registry = TestRegistries.smallRegistry();
        for (long seed = 0; seed < 300; seed++) {
            FortuneSnapshot s = DrawPipeline.draw(100, seed, null, registry, MAX_YI, MAX_JI, SIGN_PREFER);
            TierData tier = registry.tier(s.tierId());
            assertNotNull(tier, "tier " + s.tierId() + " must exist");
            if (s.signId() != null) {
                SignEntry sign = registry.sign(s.signId());
                assertTrue(tier.rank() >= sign.minRank() && tier.rank() <= sign.maxRank(),
                        "sign " + s.signId() + " range must contain tier rank " + tier.rank());
            }
            assertTrue(s.yiIds().size() <= MAX_YI, "yi count within limit");
            assertTrue(s.jiIds().size() <= MAX_JI, "ji count within limit");
        }
    }

    @Test
    void yiAndJiActionsDoNotCollide() {
        FortuneRegistry registry = TestRegistries.smallRegistry();
        for (long seed = 0; seed < 300; seed++) {
            FortuneSnapshot s = DrawPipeline.draw(100, seed, null, registry, MAX_YI, MAX_JI, SIGN_PREFER);
            Set<ActionType> yiActions = actionsOf(registry, s.yiIds());
            Set<ActionType> jiActions = actionsOf(registry, s.jiIds());
            for (ActionType a : yiActions) {
                assertFalse(jiActions.contains(a), "yi/ji must not share action " + a);
            }
        }
    }

    @Test
    void badProtectionAvoidsBadTierAfterBadDay() {
        FortuneRegistry registry = TestRegistries.smallRegistry();
        for (long seed = 0; seed < 300; seed++) {
            FortuneSnapshot s = DrawPipeline.draw(100, seed, "da_xiong", registry, MAX_YI, MAX_JI, SIGN_PREFER);
            TierData tier = registry.tier(s.tierId());
            assertTrue(tier.rank() > 1, "must not draw a bad tier after a bad day with protection, got " + s.tierId());
        }
    }

    @Test
    void drawForTierKeepsTier() {
        FortuneSnapshot s = DrawPipeline.drawForTier(5, 42L, "ss_da_ji", TestRegistries.smallRegistry(), MAX_YI, MAX_JI, SIGN_PREFER);
        assertEquals("ss_da_ji", s.tierId());
        assertEquals(5, s.dayIndex());
    }

    @Test
    void preferredSignIsDrawnMoreOften() {
        FortuneRegistry registry = TestRegistries.preferRegistry();
        int preferred = 0;
        int other = 0;
        for (long seed = 0; seed < 300; seed++) {
            String sign = registry.drawSign(new Random(seed), "xiao_ji",
                    List.of("kaikuang"), List.of(), 5);
            if ("s_mid".equals(sign)) {
                preferred++;
            } else if ("s_other".equals(sign)) {
                other++;
            }
        }
        assertTrue(preferred > other, "preferred sign should win with weight 5: " + preferred + " vs " + other);
        assertTrue(preferred > 150, "preferred sign should dominate: " + preferred);
    }

    @Test
    void restEntriesAreDrawable() {
        // effect-less / rest-only entries must still be drawable (regression for anchuang/anmian)
        FortuneRegistry registry = TestRegistries.restOnlyRegistry();
        for (long seed = 0; seed < 50; seed++) {
            FortuneSnapshot s = DrawPipeline.draw(100, seed, null, registry, 1, 1, SIGN_PREFER);
            assertTrue(s.yiIds().contains("anmian"), "rest entry must be drawable");
        }
    }

    @Test
    void weightedEntryPickedMoreOften() {
        FortuneRegistry registry = TestRegistries.weightedYiJiRegistry();
        int common = 0;
        int rare = 0;
        for (long seed = 0; seed < 300; seed++) {
            var picked = registry.drawEntries(new Random(seed), "yi", 1, new HashSet<>());
            if (picked.size() == 1) {
                if ("kaikuang".equals(picked.get(0).id())) common++;
                else if ("qiucai".equals(picked.get(0).id())) rare++;
            }
        }
        assertTrue(common > rare, "weight-5 entry should dominate weight-1 entry: " + common + " vs " + rare);
        assertTrue(common > 200, "weight-5 entry should be clearly dominant: " + common);
    }

    private static Set<ActionType> actionsOf(FortuneRegistry registry, java.util.List<String> ids) {
        Set<ActionType> actions = new HashSet<>();
        for (String id : ids) {
            YiJiEntry entry = registry.yiJi(id);
            if (entry == null) continue;
            entry.effects().keySet().forEach(k -> {
                ActionType action = ModifierTable.actionOf(k);
                if (action != null) actions.add(action);
            });
        }
        return actions;
    }
}
