package com.tanrunn.chineseoracle.server.registry;

import java.util.Map;

public record YiJiEntry(
        String id,
        String category,
        String name,
        Map<String, Float> effects,
        int weight) {
}
