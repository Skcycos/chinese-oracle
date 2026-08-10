package com.tanrunn.chineseoracle.server.registry;

public record TierData(
        String id,
        String displayName,
        int rank,
        int weight,
        float scale,
        boolean enabled) {
}
