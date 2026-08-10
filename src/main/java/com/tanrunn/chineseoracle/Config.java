package com.tanrunn.chineseoracle;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;

// Server-side configuration for the daily fortune mod.
// Mirrors the fields described in the design document (section 5.3).
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for the mod")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue CHAT_ANNOUNCE = BUILDER
            .comment("Whether to announce the daily fortune in chat when a player receives their sign")
            .define("chatAnnounce", true);

    public static final ModConfigSpec.IntValue MAX_YI = BUILDER
            .comment("Maximum number of '宜' (recommended) entries per fortune")
            .defineInRange("maxYi", 3, 0, 8);

    public static final ModConfigSpec.IntValue MAX_JI = BUILDER
            .comment("Maximum number of '忌' (avoided) entries per fortune")
            .defineInRange("maxJi", 2, 0, 8);

    public static final ModConfigSpec.DoubleValue MODIFIER_CAP = BUILDER
            .comment("Cap on fortune modifiers, e.g. 0.15 means +-15% at most")
            .defineInRange("modifierCap", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.IntValue ROLLOVER_PLAYERS_PER_TICK = BUILDER
            .comment("Max players processed per tick by the day-rollover queue (spreads the day-change spike)")
            .defineInRange("rolloverPlayersPerTick", 8, 1, 64);

    public static final ModConfigSpec.BooleanValue BAD_PROTECTION = BUILDER
            .comment("If yesterday was 大凶/下下大凶, avoid drawing a bad tier today (continuous bad luck protection)")
            .define("badProtection", true);

    public static final ModConfigSpec.IntValue REROLL_MAX_PER_DAY = BUILDER
            .comment("Maximum number of /oracle rerolls a player may use per day")
            .defineInRange("rerollMaxPerDay", 1, 0, 64);

    public static final ModConfigSpec.BooleanValue REROLL_REQUIRE_ITEM = BUILDER
            .comment("Require an incense item to be consumed when a player rerolls their fortune")
            .define("rerollRequireItem", true);

    public static final ModConfigSpec.BooleanValue SHELTER_ENABLED = BUILDER
            .comment("Enable 大凶庇护: reduce penalties while near your bed/respawn point")
            .define("shelterEnabled", true);

    public static final ModConfigSpec.DoubleValue SHELTER_RADIUS = BUILDER
            .comment("Shelter radius (blocks) around the bed/respawn point")
            .defineInRange("shelterRadius", 48.0, 1.0, 512.0);

    public static final ModConfigSpec.ConfigValue<String> SHELTER_BAD_TIER_MIN = BUILDER
            .comment("Tier at or worse than which shelter applies, e.g. da_xiong")
            .define("shelterBadTierMin", "da_xiong");

    public static final ModConfigSpec.DoubleValue SHELTER_PENALTY_SCALE = BUILDER
            .comment("Fraction of the penalty that remains while sheltered, e.g. 0.35 = only 35% penalty")
            .defineInRange("shelterPenaltyScale", 0.35, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue ENABLE_MINING = BUILDER
            .comment("Apply mining action modifiers (break speed)")
            .define("enableMining", true);

    public static final ModConfigSpec.BooleanValue ENABLE_COMBAT = BUILDER
            .comment("Apply combat action modifiers (incoming/outgoing damage)")
            .define("enableCombat", true);

    public static final ModConfigSpec.BooleanValue ENABLE_FISHING = BUILDER
            .comment("Apply fishing action modifiers (bonus catch chance)")
            .define("enableFishing", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TRADING = BUILDER
            .comment("Apply trading action modifiers (villager price adjustment while trading)")
            .define("enableTrading", true);

    public static final ModConfigSpec.ConfigValue<String> DAY_SOURCE = BUILDER
            .comment("Day rollover source: game_day (overworld day time) | wall_clock_utc (real date UTC) | wall_clock_offset (real date with time zone)")
            .define("daySource", "game_day",
                    o -> o instanceof String s && Set.of("game_day", "wall_clock_utc", "wall_clock_offset").contains(s));

    public static final ModConfigSpec.IntValue WALL_CLOCK_OFFSET_HOURS = BUILDER
            .comment("Time zone offset in hours for daySource=wall_clock_offset, e.g. 8 = UTC+8")
            .defineInRange("wallClockOffsetHours", 8, -14, 14);

    public static final ModConfigSpec.BooleanValue SKIP_SPECTATOR = BUILDER
            .comment("Do not draw fortunes for spectators")
            .define("skipSpectator", true);

    public static final ModConfigSpec.BooleanValue CREATIVE_BYPASS = BUILDER
            .comment("Creative players are not affected by penalties (still receive a fortune)")
            .define("creativeBypass", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SKIP_BOT_PREFIXES = BUILDER
            .comment("Player name prefixes treated as bots/fake players that never draw a fortune")
            .defineListAllowEmpty("skipBotNamePrefixes", List.of("[bot]"), () -> "", s -> s instanceof String);

    public static final ModConfigSpec.IntValue SHARE_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (seconds) between /oracle share broadcasts")
            .defineInRange("shareCooldownSeconds", 60, 0, 3600);

    public static final ModConfigSpec.BooleanValue METRICS = BUILDER
            .comment("Log the rolling average rollover drain time every 30 seconds")
            .define("metrics", false);

    public static final ModConfigSpec.BooleanValue SHOW_SOLAR_TERM = BUILDER
            .comment("Show the current 节气 (solar term) in the fortune display")
            .define("showSolarTerm", true);

    public static final ModConfigSpec.BooleanValue SHOW_SHICHEN = BUILDER
            .comment("Show the current 时辰 in the fortune display")
            .define("showShichen", true);

    public static final ModConfigSpec.BooleanValue SHICHEN_BONUS = BUILDER
            .comment("Apply a tiny event-driven bonus during auspicious 时辰")
            .define("shichenBonus", true);

    public static final ModConfigSpec.IntValue SHICHEN_BONUS_PERCENT = BUILDER
            .comment("Auspicious 时辰 bonus strength in percent (e.g. 3 = +3%)")
            .defineInRange("shichenBonusPercent", 3, 0, 50);

    public static final ModConfigSpec.BooleanValue FESTIVAL_BONUS = BUILDER
            .comment("Apply a small bonus on festival days")
            .define("festivalBonus", true);

    public static final ModConfigSpec.IntValue FESTIVAL_BONUS_PERCENT = BUILDER
            .comment("Festival day bonus strength in percent (e.g. 5 = +5%)")
            .defineInRange("festivalBonusPercent", 5, 0, 50);

    public static final ModConfigSpec.IntValue SIGN_PREFER_WEIGHT = BUILDER
            .comment("Weight multiplier for signs whose prefer_yi/prefer_ji match today's entries (0 disables)")
            .defineInRange("signPreferWeight", 3, 0, 50);

    public static final ModConfigSpec.BooleanValue WUXING_EFFECT_ENABLED = BUILDER
            .comment("Apply a tiny global modifier from the drawn 五行 element")
            .define("wuxingEffectEnabled", true);

    public static final ModConfigSpec.IntValue WUXING_EFFECT_PERCENT = BUILDER
            .comment("五行 effect strength in percent (e.g. 2 = +2% on hooked actions)")
            .defineInRange("wuxingEffectPercent", 2, 0, 10);

    public static final ModConfigSpec.IntValue REROLL_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown (seconds) between individual /oracle rerolls")
            .defineInRange("rerollCooldownSeconds", 30, 0, 3600);

    public static final ModConfigSpec.BooleanValue DRAW_PARTICLES = BUILDER
            .comment("Spawn a light vanilla particle burst when a fortune is drawn/rerolled")
            .define("drawParticles", true);

    public static final ModConfigSpec.BooleanValue ENABLE_REST = BUILDER
            .comment("Apply rest action effects (e.g. 安眠 heals on wake-up)")
            .define("enableRest", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
