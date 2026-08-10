package com.tanrunn.chineseoracle.server.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tanrunn.chineseoracle.common.ActionType;
import com.tanrunn.chineseoracle.server.fortune.ModifierTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;


import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FortuneRegistry {
    private static final Gson GSON = new Gson();

    private static volatile FortuneRegistry INSTANCE = new FortuneRegistry(defaultTiers(), Map.of(), Map.of(), Map.of());

    private final Map<String, TierData> tiers;
    private final Map<String, YiJiEntry> yiJi;
    private final Map<String, SignEntry> signs;
    private final Map<String, FestivalDef> festivals;
    private final Map<String, List<YiJiEntry>> byCategory;
    private final List<String> wuxing = List.of("金", "木", "水", "火", "土");

    // Public so unit tests (and datapack tooling) can construct immutable registries directly.
    public FortuneRegistry(Map<String, TierData> tiers, Map<String, YiJiEntry> yiJi, Map<String, SignEntry> signs) {
        this(tiers, yiJi, signs, Map.of());
    }

    public FortuneRegistry(Map<String, TierData> tiers, Map<String, YiJiEntry> yiJi, Map<String, SignEntry> signs,
                           Map<String, FestivalDef> festivals) {
        this.tiers = new LinkedHashMap<>(tiers);
        this.yiJi = new LinkedHashMap<>(yiJi);
        this.signs = new LinkedHashMap<>(signs);
        this.festivals = new LinkedHashMap<>(festivals);
        Map<String, List<YiJiEntry>> byCat = new LinkedHashMap<>();
        for (YiJiEntry entry : yiJi.values()) {
            byCat.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
        }
        this.byCategory = byCat;
    }

    public static FortuneRegistry get() {
        return INSTANCE;
    }

    static void set(FortuneRegistry registry) {
        INSTANCE = registry;
    }

    public static void reload(ResourceManager manager) {
        set(load(manager));
    }

    // ---- defaults (design document section 3.2) ----
    private static Map<String, TierData> defaultTiers() {
        Map<String, TierData> tiers = new LinkedHashMap<>();
        addTier(tiers, "ss_da_ji", "上上大吉", 8, 1, 1.3f);
        addTier(tiers, "da_ji", "大吉", 7, 2, 1.1f);
        addTier(tiers, "zhong_ji", "中吉", 6, 4, 0.9f);
        addTier(tiers, "xiao_ji", "小吉", 5, 8, 0.6f);
        addTier(tiers, "ping", "平", 4, 10, 0.3f);
        addTier(tiers, "xiao_xiong", "小凶", 3, 6, -0.6f);
        addTier(tiers, "zhong_xiong", "中凶", 2, 3, -0.9f);
        addTier(tiers, "da_xiong", "大凶", 1, 2, -1.1f);
        addTier(tiers, "xx_da_xiong", "下下大凶", 0, 1, -1.3f);
        return tiers;
    }

    private static void addTier(Map<String, TierData> tiers, String id, String name, int rank, int weight, float scale) {
        tiers.put(id, new TierData(id, name, rank, weight, scale, true));
    }

    // ---- datapack loading ----
    public static FortuneRegistry load(ResourceManager manager) {
        FortuneRegistry reg = new FortuneRegistry(defaultTiers(), Map.of(), Map.of(), Map.of());
        reg.loadTierOverrides(manager);
        reg.loadYiJi(manager);
        reg.loadSigns(manager);
        reg.loadFestivals(manager);
        return reg;
    }

    private void loadTierOverrides(ResourceManager manager) {
        listJsons(manager, "fortune_tier").forEach((id, obj) -> {
            TierData base = tiers.get(id);
            if (base == null) return;
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : base.weight();
            boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            tiers.put(id, new TierData(base.id(), base.displayName(), base.rank(), weight, base.scale(), enabled));
        });
    }

    private void loadYiJi(ResourceManager manager) {
        listJsons(manager, "yi_ji").forEach((id, obj) -> {
            String category = getString(obj, "category", "yi");
            String name = getString(obj, "name", id);
            Map<String, Float> effects = new LinkedHashMap<>();
            if (obj.has("effects") && obj.get("effects").isJsonObject()) {
                obj.getAsJsonObject("effects").entrySet().forEach(e -> effects.put(e.getKey(), e.getValue().getAsFloat()));
            }
            int weight = obj.has("weight") ? Math.max(1, obj.get("weight").getAsInt()) : 1;
            yiJi.put(id, new YiJiEntry(id, category, name, effects, weight));
        });
    }

    private void loadSigns(ResourceManager manager) {
        listJsons(manager, "signs").forEach((id, obj) -> {
            String minTier = getString(obj, "min_tier", "xx_da_xiong");
            String maxTier = getString(obj, "max_tier", "ss_da_ji");
            int minRank = tiers.containsKey(minTier) ? tiers.get(minTier).rank() : 0;
            int maxRank = tiers.containsKey(maxTier) ? tiers.get(maxTier).rank() : 8;
            List<String> poem = new ArrayList<>();
            if (obj.has("poem") && obj.get("poem").isJsonArray()) {
                obj.getAsJsonArray("poem").forEach(p -> poem.add(p.getAsString()));
            }
            String explain = getString(obj, "explain", "");
            List<String> preferYi = readStringList(obj, "prefer_yi");
            List<String> preferJi = readStringList(obj, "prefer_ji");
            signs.put(id, new SignEntry(id, Math.min(minRank, maxRank), Math.max(minRank, maxRank),
                    poem, explain, preferYi, preferJi));
        });
    }

    private static List<String> readStringList(JsonObject obj, String key) {
        List<String> result = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            obj.getAsJsonArray(key).forEach(e -> {
                if (e.isJsonPrimitive()) result.add(e.getAsString());
            });
        }
        return result;
    }

    private void loadFestivals(ResourceManager manager) {
        listJsons(manager, "festivals").forEach((id, obj) -> {
            String name = getString(obj, "name", id);
            int dayOfYear = obj.has("day_of_year") ? obj.get("day_of_year").getAsInt() : 0;
            int days = obj.has("days") ? obj.get("days").getAsInt() : 1;
            festivals.put(id, new FestivalDef(id, name, Math.floorMod(dayOfYear, 365), Math.max(1, days)));
        });
    }

    private Map<String, JsonObject> listJsons(ResourceManager manager, String directory) {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        String prefix = directory + "/";
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(directory, p -> p.getPath().endsWith(".json")).entrySet()) {
            String path = entry.getKey().getPath();
            String id = path.substring(prefix.length(), path.length() - ".json".length());
            try (var in = entry.getValue().open(); var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element != null && element.isJsonObject()) {
                    result.put(id, element.getAsJsonObject());
                }
            } catch (Exception e) {
                com.tanrunn.chineseoracle.ChineseOracleMod.LOGGER.error("Failed to load {} datapack entry {}", directory, id, e);
            }
        }
        return result;
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : defaultValue;
    }

    // ---- queries used by draw pipeline & commands ----
    public TierData tier(String id) {
        return tiers.get(id);
    }

    public List<String> tierIds() {
        return new ArrayList<>(tiers.keySet());
    }

    public boolean hasTier(String id) {
        return tiers.containsKey(id);
    }

    public float tierScale(String id) {
        TierData tier = tiers.get(id);
        return tier == null ? 0f : tier.scale();
    }

    public boolean isBadTier(String id) {
        TierData tier = tiers.get(id);
        return tier != null && tier.rank() <= 1;
    }

    public boolean isWorseOrEqual(String tierId, String minTierId) {
        TierData tier = tiers.get(tierId);
        TierData min = tiers.get(minTierId);
        return tier != null && min != null && tier.rank() <= min.rank();
    }

    public YiJiEntry yiJi(String id) {
        return yiJi.get(id);
    }

    public SignEntry sign(String id) {
        return signs.get(id);
    }

    public int tierCount() {
        return tiers.size();
    }

    public int yiJiCount() {
        return yiJi.size();
    }

    public int signCount() {
        return signs.size();
    }

    public int festivalCount() {
        return festivals.size();
    }

    public String currentFestival(long dayIndex) {
        long dayOfYear = Math.floorMod(dayIndex, 365L);
        for (FestivalDef f : festivals.values()) {
            if (dayOfYear >= f.dayOfYear() && dayOfYear < f.dayOfYear() + f.days()) {
                return f.name();
            }
        }
        return null;
    }

    // ---- weighted draws ----
    public String drawTier(java.util.Random random, String yesterdayTier, boolean badProtection) {
        List<TierData> eligible = tiers.values().stream().filter(TierData::enabled).toList();
        if (eligible.isEmpty()) return "ping";
        String pick = weightedPick(random, eligible).id();
        if (badProtection && yesterdayTier != null && isBadTier(yesterdayTier) && isBadTier(pick)) {
            List<TierData> notBad = eligible.stream().filter(t -> !isBadTier(t.id())).toList();
            if (!notBad.isEmpty()) {
                pick = weightedPick(random, notBad).id();
            }
        }
        return pick;
    }

    private static TierData weightedPick(java.util.Random random, List<TierData> pool) {
        int total = pool.stream().mapToInt(TierData::weight).sum();
        int r = random.nextInt(Math.max(1, total));
        int acc = 0;
        for (TierData tier : pool) {
            acc += tier.weight();
            if (r < acc) return tier;
        }
        return pool.get(pool.size() - 1);
    }

    public List<YiJiEntry> drawEntries(java.util.Random random, String category, int max, java.util.Set<ActionType> usedActions) {
        List<YiJiEntry> pool = new ArrayList<>(byCategory.getOrDefault(category, List.of()));
        List<YiJiEntry> picked = new ArrayList<>();
        for (int i = 0; i < max && !pool.isEmpty(); i++) {
            List<YiJiEntry> eligible = pool.stream()
                    .filter(e -> isEligible(e, usedActions))
                    .toList();
            if (eligible.isEmpty()) break;
            YiJiEntry pick = weightedEntryPick(random, eligible);
            picked.add(pick);
            pool.remove(pick);
            pick.effects().keySet().forEach(k -> {
                ActionType action = ModifierTable.actionOf(k);
                if (action != null) usedActions.add(action);
            });
        }
        return picked;
    }

    private static boolean isEligible(YiJiEntry entry, java.util.Set<ActionType> usedActions) {
        List<ActionType> actions = entry.effects().keySet().stream()
                .map(ModifierTable::actionOf)
                .filter(Objects::nonNull)
                .toList();
        if (actions.isEmpty()) return true;
        return actions.stream().anyMatch(a -> !usedActions.contains(a));
    }

    private static YiJiEntry weightedEntryPick(java.util.Random random, List<YiJiEntry> pool) {
        int total = 0;
        for (YiJiEntry entry : pool) {
            total += entry.weight();
        }
        int r = random.nextInt(Math.max(1, total));
        int acc = 0;
        for (YiJiEntry entry : pool) {
            acc += entry.weight();
            if (r < acc) return entry;
        }
        return pool.get(pool.size() - 1);
    }

    public String drawSign(java.util.Random random, String tierId, List<String> yiIds, List<String> jiIds, int preferWeight) {
        TierData tier = tiers.get(tierId);
        if (tier == null || signs.isEmpty()) return null;
        List<SignEntry> pool = signs.values().stream()
                .filter(s -> s.minRank() <= tier.rank() && tier.rank() <= s.maxRank())
                .toList();
        if (pool.isEmpty()) pool = new ArrayList<>(signs.values());
        int total = 0;
        for (SignEntry sign : pool) {
            total += signWeight(sign, yiIds, jiIds, preferWeight);
        }
        int r = random.nextInt(Math.max(1, total));
        int acc = 0;
        for (SignEntry sign : pool) {
            acc += signWeight(sign, yiIds, jiIds, preferWeight);
            if (r < acc) return sign.id();
        }
        return pool.get(pool.size() - 1).id();
    }

    private static int signWeight(SignEntry sign, List<String> yiIds, List<String> jiIds, int preferWeight) {
        if (preferWeight > 0 && (matchesAny(sign.preferYi(), yiIds) || matchesAny(sign.preferJi(), jiIds))) {
            return preferWeight;
        }
        return 1;
    }

    private static boolean matchesAny(List<String> prefers, List<String> drawn) {
        if (prefers.isEmpty() || drawn.isEmpty()) return false;
        for (String prefer : prefers) {
            if (drawn.contains(prefer)) return true;
        }
        return false;
    }

    public String randomWuxing(java.util.Random random) {
        return wuxing.get(random.nextInt(wuxing.size()));
    }
}
