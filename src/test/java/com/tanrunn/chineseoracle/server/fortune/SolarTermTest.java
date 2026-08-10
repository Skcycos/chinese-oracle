package com.tanrunn.chineseoracle.server.fortune;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SolarTerm tests: solar-longitude model boundaries and year wrap-around. */
class SolarTermTest {

    @Test
    void yearStartsAtLichun() {
        assertEquals("立春", SolarTerm.forDayIndex(0));
    }

    @Test
    void termBoundaries() {
        assertEquals("立春", SolarTerm.forDayIndex(13));
        assertEquals("雨水", SolarTerm.forDayIndex(14));
        assertEquals("惊蛰", SolarTerm.forDayIndex(43));
        assertEquals("春分", SolarTerm.forDayIndex(44));
        assertEquals("清明", SolarTerm.forDayIndex(59));
        assertEquals("立夏", SolarTerm.forDayIndex(90));
        assertEquals("夏至", SolarTerm.forDayIndex(137));
        assertEquals("秋分", SolarTerm.forDayIndex(231));
        assertEquals("冬至", SolarTerm.forDayIndex(321));
        assertEquals("小寒", SolarTerm.forDayIndex(335));
        assertEquals("大寒", SolarTerm.forDayIndex(350));
        assertEquals("大寒", SolarTerm.forDayIndex(364));
    }

    @Test
    void wrapsAcrossYears() {
        assertEquals("立春", SolarTerm.forDayIndex(365));
        assertEquals("立春", SolarTerm.forDayIndex(730));
    }

    @Test
    void visitsAllTermsInOrder() {
        List<String> terms = List.of("立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
                "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
                "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
                "立冬", "小雪", "大雪", "冬至", "小寒", "大寒");
        int prev = -1;
        for (long d = 0; d < 365; d++) {
            int index = terms.indexOf(SolarTerm.forDayIndex(d));
            if (prev >= 0) {
                int expected = (prev + 1) % 24;
                assertTrue(index == prev || index == expected,
                        "term must stay or advance by one at day " + d);
            }
            prev = index;
        }
        assertEquals(23, prev, "year ends with 大寒");
        assertEquals("立春", SolarTerm.forDayIndex(365), "next year starts over");
    }
}
