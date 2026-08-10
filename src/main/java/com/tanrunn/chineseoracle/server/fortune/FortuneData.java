package com.tanrunn.chineseoracle.server.fortune;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent per-player fortune state, stored as a player attachment
 * (design document section 5.1).
 */
public class FortuneData implements INBTSerializable<CompoundTag> {
    public static final int SCHEMA_VERSION = 1;

    public int schemaVersion = SCHEMA_VERSION;
    public long dayIndex = -1;
    public String tierId;
    public List<String> yiIds = new ArrayList<>();
    public List<String> jiIds = new ArrayList<>();
    public String signId;
    public String wuxing;
    public long seed;
    public int rerollsUsed;
    public int flags;

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schemaVersion", schemaVersion);
        tag.putLong("dayIndex", dayIndex);
        if (tierId != null) tag.putString("tierId", tierId);
        tag.put("yiIds", toListTag(yiIds));
        tag.put("jiIds", toListTag(jiIds));
        if (signId != null) tag.putString("signId", signId);
        if (wuxing != null) tag.putString("wuxing", wuxing);
        tag.putLong("seed", seed);
        tag.putInt("rerollsUsed", rerollsUsed);
        tag.putInt("flags", flags);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        schemaVersion = tag.getInt("schemaVersion");
        dayIndex = tag.getLong("dayIndex");
        tierId = tag.contains("tierId") ? tag.getString("tierId") : null;
        yiIds = fromListTag(tag.getList("yiIds", Tag.TAG_STRING));
        jiIds = fromListTag(tag.getList("jiIds", Tag.TAG_STRING));
        signId = tag.contains("signId") ? tag.getString("signId") : null;
        wuxing = tag.contains("wuxing") ? tag.getString("wuxing") : null;
        seed = tag.getLong("seed");
        rerollsUsed = tag.getInt("rerollsUsed");
        flags = tag.getInt("flags");
    }

    private static ListTag toListTag(List<String> values) {
        ListTag list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v)));
        return list;
    }

    private static List<String> fromListTag(ListTag list) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }
}
