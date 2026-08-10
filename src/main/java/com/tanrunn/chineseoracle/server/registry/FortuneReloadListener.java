package com.tanrunn.chineseoracle.server.registry;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class FortuneReloadListener extends SimplePreparableReloadListener<FortuneRegistry> {
    @Override
    protected FortuneRegistry prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return FortuneRegistry.load(resourceManager);
    }

    @Override
    protected void apply(FortuneRegistry data, ResourceManager resourceManager, ProfilerFiller profiler) {
        FortuneRegistry.set(data);
        com.tanrunn.chineseoracle.ChineseOracleMod.LOGGER.info("Fortune registry loaded: {} tiers, {} yi/ji entries, {} signs, {} festivals",
                data.tierCount(), data.yiJiCount(), data.signCount(), data.festivalCount());
    }
}
