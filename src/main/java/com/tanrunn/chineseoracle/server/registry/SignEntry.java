package com.tanrunn.chineseoracle.server.registry;

import java.util.List;

public record SignEntry(
        String id,
        int minRank,
        int maxRank,
        List<String> poem,
        String explain,
        List<String> preferYi,
        List<String> preferJi) {
}
