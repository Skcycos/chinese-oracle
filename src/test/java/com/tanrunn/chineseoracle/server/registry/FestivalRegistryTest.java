package com.tanrunn.chineseoracle.server.registry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Datapack-driven festival lookup tests. */
class FestivalRegistryTest {

    @Test
    void currentFestivalMatchesWindow() {
        Map<String, FestivalDef> festivals = Map.of("cny", new FestivalDef("cny", "春节", 0, 3));
        FortuneRegistry reg = new FortuneRegistry(Map.of(), Map.of(), Map.of(), festivals);

        assertEquals("春节", reg.currentFestival(0));
        assertEquals("春节", reg.currentFestival(2));
        assertNull(reg.currentFestival(3));
        assertNull(reg.currentFestival(364));
    }

    @Test
    void wrapsDayOfYearAcrossYears() {
        Map<String, FestivalDef> festivals = Map.of("eve", new FestivalDef("eve", "除夕", 363, 2));
        FortuneRegistry reg = new FortuneRegistry(Map.of(), Map.of(), Map.of(), festivals);

        assertEquals("除夕", reg.currentFestival(363));
        assertEquals("除夕", reg.currentFestival(364));
        assertEquals("除夕", reg.currentFestival(728)); // floorMod(728, 365) = 363
        assertNull(reg.currentFestival(362));
    }

    @Test
    void emptyFestivalsMatchNothing() {
        FortuneRegistry reg = new FortuneRegistry(Map.of(), Map.of(), Map.of());
        assertNull(reg.currentFestival(0));
    }
}
