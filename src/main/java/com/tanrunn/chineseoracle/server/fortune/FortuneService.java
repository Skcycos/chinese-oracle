package com.tanrunn.chineseoracle.server.fortune;

import com.mojang.logging.LogUtils;
import com.tanrunn.chineseoracle.ChineseOracleMod;
import com.tanrunn.chineseoracle.Config;
import com.tanrunn.chineseoracle.common.FortuneSnapshot;
import com.tanrunn.chineseoracle.common.Modifier;
import com.tanrunn.chineseoracle.common.network.FortuneDisplay;
import com.tanrunn.chineseoracle.server.registry.FortuneRegistry;
import com.tanrunn.chineseoracle.server.registry.SignEntry;
import com.tanrunn.chineseoracle.server.registry.TierData;
import com.tanrunn.chineseoracle.server.registry.YiJiEntry;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative fortune service (sections 4.1, 5.1, 9.1). All drawing,
 * storage and modifier computation happens only here and only on the logical server.
 */
public final class FortuneService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SALT = 0x9E3779B97F4A7C15L;
    private static final Map<UUID, Cached> CACHE = new HashMap<>();
    private static final Map<UUID, Long> LAST_REROLL = new HashMap<>();

    private record Cached(long dayIndex, FortuneSnapshot snapshot, Modifier modifier) {
    }

    private FortuneService() {
    }

    public static FortuneSnapshot ensureToday(ServerPlayer player) {
        if (shouldSkipDraw(player)) {
            return neutralSnapshot(player);
        }
        FortuneData data = player.getData(ChineseOracleMod.FORTUNE_DATA.get());
        long dayIndex = DayService.currentDayIndex(player.server);

        if (data.dayIndex == dayIndex) {
            return snapshotOf(data);
        }

        String yesterdayTier = data.tierId;
        long seed = mixSeed(player, dayIndex);
        FortuneSnapshot snapshot = DrawPipeline.draw(dayIndex, seed, yesterdayTier,
                FortuneRegistry.get(), Config.MAX_YI.get(), Config.MAX_JI.get(), Config.SIGN_PREFER_WEIGHT.get());

        applyToData(data, snapshot);
        player.setData(ChineseOracleMod.FORTUNE_DATA.get(), data);

        if (Config.CHAT_ANNOUNCE.get()) {
            player.sendSystemMessage(Component.literal(formatSnapshot(snapshot, player.server)));
            data.flags |= 1;
            player.setData(ChineseOracleMod.FORTUNE_DATA.get(), data);
        }
        spawnDrawParticles(player);

        cache(player, snapshot);
        LOGGER.debug("Drew fortune for {}: day={} tier={}", player.getName().getString(), dayIndex, snapshot.tierId());
        return snapshot;
    }

    public static boolean reroll(ServerPlayer player) {
        FortuneData data = player.getData(ChineseOracleMod.FORTUNE_DATA.get());
        long dayIndex = DayService.currentDayIndex(player.server);
        if (data.dayIndex != dayIndex) {
            ensureToday(player);
            return false;
        }
        if (data.rerollsUsed >= Config.REROLL_MAX_PER_DAY.get()) {
            player.sendSystemMessage(Component.literal("今日改签次数已用完。"));
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_REROLL.get(player.getUUID());
        int cooldownMs = Config.REROLL_COOLDOWN_SECONDS.get() * 1000;
        if (last != null && now - last < cooldownMs) {
            player.sendSystemMessage(Component.literal("改签冷却中，请稍后再试。"));
            return false;
        }
        long seed = HashCommon.mix(data.seed + SALT);
        FortuneSnapshot snapshot = DrawPipeline.drawForTier(dayIndex, seed, data.tierId,
                FortuneRegistry.get(), Config.MAX_YI.get(), Config.MAX_JI.get(), Config.SIGN_PREFER_WEIGHT.get());
        applyToData(data, snapshot);
        data.seed = seed;
        data.rerollsUsed = data.rerollsUsed + 1;
        player.setData(ChineseOracleMod.FORTUNE_DATA.get(), data);
        player.sendSystemMessage(Component.literal(formatSnapshot(snapshot, player.server)));
        LAST_REROLL.put(player.getUUID(), now);
        spawnDrawParticles(player);
        cache(player, snapshot);
        return true;
    }

    public static boolean forceSet(ServerPlayer player, String tierId) {
        FortuneRegistry registry = FortuneRegistry.get();
        if (!registry.hasTier(tierId)) {
            return false;
        }
        FortuneData data = player.getData(ChineseOracleMod.FORTUNE_DATA.get());
        long dayIndex = DayService.currentDayIndex(player.server);
        long seed = data.seed != 0 ? HashCommon.mix(data.seed + SALT) : mixSeed(player, dayIndex);
        FortuneSnapshot snapshot = DrawPipeline.drawForTier(dayIndex, seed, tierId,
                registry, Config.MAX_YI.get(), Config.MAX_JI.get(), Config.SIGN_PREFER_WEIGHT.get());
        applyToData(data, snapshot);
        data.seed = seed;
        player.setData(ChineseOracleMod.FORTUNE_DATA.get(), data);
        player.sendSystemMessage(Component.literal(formatSnapshot(snapshot, player.server)));
        spawnDrawParticles(player);
        cache(player, snapshot);
        return true;
    }

    public static void reload(MinecraftServer server) {
        FortuneRegistry.reload(server.getResourceManager());
        CACHE.clear();
    }

    public static Modifier getModifier(ServerPlayer player) {
        if (Config.CREATIVE_BYPASS.get() && player.isCreative()) {
            ensureToday(player);
            return Modifier.neutral();
        }
        return getCached(player).modifier();
    }

    public static FortuneSnapshot getSnapshot(ServerPlayer player) {
        return getCached(player).snapshot();
    }

    public static FortuneDisplay toDisplay(ServerPlayer player, FortuneSnapshot snapshot) {
        FortuneRegistry registry = FortuneRegistry.get();
        TierData tier = registry.tier(snapshot.tierId());
        SignEntry sign = registry.sign(snapshot.signId());
        return new FortuneDisplay(
                snapshot.dayIndex(),
                tier != null ? tier.displayName() : snapshot.tierId(),
                tier != null ? tier.rank() : 4,
                namesOf(registry, snapshot.yiIds()),
                namesOf(registry, snapshot.jiIds()),
                sign != null && !sign.poem().isEmpty() ? String.join("\n", sign.poem()) : null,
                sign != null ? sign.explain() : null,
                snapshot.wuxing(),
                Config.SHOW_SOLAR_TERM.get() ? SolarTerm.forDayIndex(snapshot.dayIndex()) : null,
                Shichen.name(player.server),
                Shichen.isAuspicious(player.server),
                Festival.current(snapshot.dayIndex()));
    }

    private static List<String> namesOf(FortuneRegistry registry, List<String> ids) {
        List<String> names = new ArrayList<>(ids.size());
        for (String id : ids) {
            YiJiEntry entry = registry.yiJi(id);
            names.add(entry != null ? entry.name() : id);
        }
        return names;
    }

    public static void removeCache(UUID uuid) {
        CACHE.remove(uuid);
        LAST_REROLL.remove(uuid);
    }

    private static void spawnDrawParticles(ServerPlayer player) {
        if (!Config.DRAW_PARTICLES.get()) return;
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 0.6;
        double z = player.getZ();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 8, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, x, y + 0.5, z, 6, 0.4, 0.5, 0.4, 0.1);
    }

    private static boolean shouldSkipDraw(ServerPlayer player) {
        if (Config.SKIP_SPECTATOR.get() && player.isSpectator()) return true;
        for (String prefix : Config.SKIP_BOT_PREFIXES.get()) {
            if (player.getName().getString().startsWith(prefix)) return true;
        }
        return false;
    }

    private static FortuneSnapshot neutralSnapshot(ServerPlayer player) {
        return new FortuneSnapshot(DayService.currentDayIndex(player.server), "ping",
                List.of(), List.of(), null, null, 0, 0);
    }

    private static Cached getCached(ServerPlayer player) {
        Cached cached = CACHE.get(player.getUUID());
        long dayIndex = DayService.currentDayIndex(player.server);
        if (cached == null || cached.dayIndex() != dayIndex) {
            FortuneSnapshot snapshot = ensureToday(player);
            cached = new Cached(dayIndex, snapshot,
                    ModifierTable.build(snapshot, FortuneRegistry.get(), Config.MODIFIER_CAP.get().floatValue()));
            CACHE.put(player.getUUID(), cached);
        }
        return cached;
    }

    private static void cache(ServerPlayer player, FortuneSnapshot snapshot) {
        CACHE.put(player.getUUID(), new Cached(snapshot.dayIndex(), snapshot,
                ModifierTable.build(snapshot, FortuneRegistry.get(), Config.MODIFIER_CAP.get().floatValue())));
    }

    private static void applyToData(FortuneData data, FortuneSnapshot snapshot) {
        data.dayIndex = snapshot.dayIndex();
        data.tierId = snapshot.tierId();
        data.yiIds = new ArrayList<>(snapshot.yiIds());
        data.jiIds = new ArrayList<>(snapshot.jiIds());
        data.signId = snapshot.signId();
        data.wuxing = snapshot.wuxing();
        data.seed = snapshot.seed();
        data.rerollsUsed = snapshot.rerollsUsed();
        data.flags = 0;
    }

    private static FortuneSnapshot snapshotOf(FortuneData data) {
        return new FortuneSnapshot(data.dayIndex, data.tierId, data.yiIds, data.jiIds,
                data.signId, data.wuxing, data.seed, data.rerollsUsed);
    }

    private static long mixSeed(ServerPlayer player, long dayIndex) {
        long worldSeed = player.server.overworld().getSeed();
        return HashCommon.mix(worldSeed ^ player.getUUID().getMostSignificantBits()
                ^ HashCommon.mix(player.getUUID().getLeastSignificantBits() ^ dayIndex)
                ^ SALT);
    }

    public static String formatSnapshot(FortuneSnapshot snapshot, MinecraftServer server) {
        FortuneRegistry registry = FortuneRegistry.get();
        TierData tier = registry.tier(snapshot.tierId());
        StringBuilder sb = new StringBuilder();
        sb.append("§e=== 今日黄历");
        if (Config.SHOW_SOLAR_TERM.get()) {
            sb.append(" · §5").append(SolarTerm.forDayIndex(snapshot.dayIndex())).append("§e");
        }
        if (Config.SHOW_SHICHEN.get()) {
            sb.append(" · §3").append(Shichen.name(server));
            if (Shichen.isAuspicious(server)) {
                sb.append("§a（吉）§e");
            } else {
                sb.append("§e");
            }
        }
        String festival = Festival.current(snapshot.dayIndex());
        if (festival != null) {
            sb.append(" · §d").append(festival).append("§e");
        }
        sb.append(" ===");
        sb.append("\n§6吉凶§r：").append(tier != null ? tier.displayName() : snapshot.tierId());
        if (snapshot.wuxing() != null) {
            sb.append("  §6五行§r：").append(snapshot.wuxing());
        }
        if (!snapshot.yiIds().isEmpty()) {
            sb.append("\n§a宜§r：").append(joinNames(registry, snapshot.yiIds()));
        }
        if (!snapshot.jiIds().isEmpty()) {
            sb.append("\n§c忌§r：").append(joinNames(registry, snapshot.jiIds()));
        }
        SignEntry sign = registry.sign(snapshot.signId());
        if (sign != null) {
            sb.append("\n§7签文§r：");
            for (String line : sign.poem()) {
                sb.append("\n  ").append(line);
            }
            if (!sign.explain().isEmpty()) {
                sb.append("\n§7解签§r：").append(sign.explain());
            }
        }
        return sb.toString();
    }

    private static String joinNames(FortuneRegistry registry, java.util.List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (sb.length() > 0) sb.append("、");
            YiJiEntry entry = registry.yiJi(id);
            sb.append(entry != null ? entry.name() : id);
        }
        return sb.toString();
    }
}
