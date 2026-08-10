package com.tanrunn.chineseoracle;

import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.SignEntry;
import com.tanrunn.chineseoracle.server.registry.TierData;
import com.tanrunn.chineseoracle.server.registry.YiJiEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared test fixtures: a small but representative registry. */
public final class TestRegistries {
    private TestRegistries() {
    }

    public static FortuneRegistry smallRegistry() {
        return new FortuneRegistry(tiers(), yiJi(), signs());
    }

    /** Registry whose mid-tier sign pool prefers the 开矿 yi entry (for prefer-weight tests). */
    public static FortuneRegistry preferRegistry() {
        Map<String, SignEntry> signs = new LinkedHashMap<>();
        signs.put("s_mid", new SignEntry("s_mid", 3, 5, List.of("细雨润物", "静待时机"), "平", List.of("kaikuang"), List.of()));
        signs.put("s_other", new SignEntry("s_other", 3, 5, List.of("浮云过眼", "随缘即安"), "平", List.of(), List.of()));
        return new FortuneRegistry(tiers(), yiJi(), signs);
    }

    private static Map<String, TierData> tiers() {
        Map<String, TierData> tiers = new LinkedHashMap<>();
        tiers.put("ss_da_ji", new TierData("ss_da_ji", "上上大吉", 8, 1, 1.3f, true));
        tiers.put("xiao_ji", new TierData("xiao_ji", "小吉", 5, 8, 0.6f, true));
        tiers.put("ping", new TierData("ping", "平", 4, 10, 0.3f, true));
        tiers.put("xiao_xiong", new TierData("xiao_xiong", "小凶", 3, 6, -0.6f, true));
        tiers.put("da_xiong", new TierData("da_xiong", "大凶", 1, 2, -1.1f, true));
        return tiers;
    }

    private static Map<String, YiJiEntry> yiJi() {
        Map<String, YiJiEntry> yiJi = new LinkedHashMap<>();
        yiJi.put("kaikuang", new YiJiEntry("kaikuang", "yi", "开矿", Map.of("mining_speed", 0.10f), 1));
        yiJi.put("dongtu", new YiJiEntry("dongtu", "ji", "忌动土", Map.of("mining_speed", 0.10f), 1));
        yiJi.put("zhengfa", new YiJiEntry("zhengfa", "yi", "征伐", Map.of("outgoing_damage", 0.08f), 1));
        yiJi.put("zhengdou", new YiJiEntry("zhengdou", "ji", "忌争斗", Map.of("outgoing_damage", 0.08f), 1));
        yiJi.put("qiucai", new YiJiEntry("qiucai", "yi", "求财", Map.of("trade_price", -0.05f), 1));
        yiJi.put("anmian", new YiJiEntry("anmian", "yi", "安眠", Map.of("rest_heal", 2.0f), 1));
        yiJi.put("anchuang", new YiJiEntry("anchuang", "ji", "忌安床", Map.of("rest_heal", -0.5f), 1));
        return yiJi;
    }

    /** Registry whose yi pool is only rest entries (no actionable effects). */
    public static FortuneRegistry restOnlyRegistry() {
        Map<String, YiJiEntry> yiJi = new LinkedHashMap<>();
        yiJi.put("anmian", new YiJiEntry("anmian", "yi", "安眠", Map.of("rest_heal", 2.0f), 1));
        return new FortuneRegistry(tiers(), yiJi, signs());
    }

    /** Registry with an intentionally rare (weight 1) and a common (weight 5) yi entry. */
    public static FortuneRegistry weightedYiJiRegistry() {
        Map<String, YiJiEntry> yiJi = new LinkedHashMap<>();
        yiJi.put("kaikuang", new YiJiEntry("kaikuang", "yi", "开矿", Map.of("mining_speed", 0.10f), 5));
        yiJi.put("qiucai", new YiJiEntry("qiucai", "yi", "求财", Map.of("trade_price", -0.05f), 1));
        return new FortuneRegistry(tiers(), yiJi, signs());
    }

    private static Map<String, SignEntry> signs() {
        Map<String, SignEntry> signs = new LinkedHashMap<>();
        signs.put("s_good", new SignEntry("s_good", 6, 8, List.of("青云有路", "贵人相助"), "吉", List.of(), List.of()));
        signs.put("s_mid", new SignEntry("s_mid", 3, 5, List.of("细雨润物", "静待时机"), "平", List.of(), List.of()));
        signs.put("s_bad", new SignEntry("s_bad", 0, 3, List.of("夜行持灯", "步步为营"), "凶", List.of(), List.of()));
        return signs;
    }
}
