package com.tanrunn.chineseoracle.server.fortune;

import com.tanrunn.chineseoracle.common.ActionType;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.YiJiEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure, deterministic weighted draw pipeline (section 6.3). Same seed always
 * produces the same sign; different players on the same day differ. All
 * configuration values are passed in explicitly so the pipeline stays a pure
 * function and is fully unit-testable. Uses java.util.Random so the logic has
 * no Minecraft runtime dependency.
 */
public final class DrawPipeline {
    private DrawPipeline() {
    }

    public static FortuneSnapshot draw(long dayIndex, long seed, String yesterdayTier,
                                       FortuneRegistry registry, int maxYi, int maxJi, int signPreferWeight) {
        Random random = new Random(seed);
        String tier = registry.drawTier(random, yesterdayTier, true);
        return drawForTier(dayIndex, seed, tier, registry, maxYi, maxJi, signPreferWeight);
    }

    public static FortuneSnapshot drawForTier(long dayIndex, long seed, String tierId,
                                              FortuneRegistry registry, int maxYi, int maxJi, int signPreferWeight) {
        Random random = new Random(seed);

        Set<ActionType> usedActions = new HashSet<>();
        List<YiJiEntry> yi = registry.drawEntries(random, "yi", maxYi, usedActions);
        List<YiJiEntry> ji = registry.drawEntries(random, "ji", maxJi, usedActions);

        List<String> yiIds = yi.stream().map(YiJiEntry::id).toList();
        List<String> jiIds = ji.stream().map(YiJiEntry::id).toList();
        String signId = registry.drawSign(random, tierId, yiIds, jiIds, signPreferWeight);
        String wuxing = registry.randomWuxing(random);

        return new FortuneSnapshot(dayIndex, tierId, yiIds, jiIds, signId, wuxing, seed, 0);
    }
}
