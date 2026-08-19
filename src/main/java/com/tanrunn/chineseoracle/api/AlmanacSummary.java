package com.tanrunn.chineseoracle.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 今日黄历摘要（只读、展示就绪）。
 *
 * <p>由 {@link ChineseOracleApi#summary} 在服务端主线程生成：名称字段均为
 * 注册表解析后的展示名称（非 tierId/yiId/jiId）。List 字段在构造时防御性复制
 * 且不可变；{@code tierName} 与 {@code shichen} 保证非 null。
 * 本类型不是网络 payload，也不暴露任何 AUI 类型。</p>
 */
public record AlmanacSummary(
        long dayIndex,
        String tierName,
        int tierRank,
        List<String> yiNames,
        List<String> jiNames,
        @Nullable String wuxing,
        @Nullable String solarTerm,
        String shichen,
        boolean shichenAuspicious,
        @Nullable String festival) {

    public AlmanacSummary {
        tierName = tierName == null ? "" : tierName;
        shichen = shichen == null ? "" : shichen;
        yiNames = List.copyOf(yiNames);
        jiNames = List.copyOf(jiNames);
    }
}
