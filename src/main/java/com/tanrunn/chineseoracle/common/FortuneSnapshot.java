package com.tanrunn.chineseoracle.common;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record FortuneSnapshot(
        long dayIndex,
        String tierId,
        List<String> yiIds,
        List<String> jiIds,
        @Nullable String signId,
        @Nullable String wuxing,
        long seed,
        int rerollsUsed) {
}
