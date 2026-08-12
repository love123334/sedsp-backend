package com.example.secdsp.modules.dss.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DssHolidayCalendarTest {

    @Test
    void holidaysBetween_includesDoubleDateAndMegaSale() {
        var hits = DssHolidayCalendar.holidaysBetween(
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2025, 11, 30)
        );
        assertTrue(hits.stream().anyMatch(h -> h.code().equals("SINGLE_11")));
        assertTrue(hits.stream().anyMatch(h -> h.code().equals("BLACK_FRIDAY")));
        assertTrue(hits.stream().anyMatch(h -> h.code().startsWith("DOUBLE_")));
    }

    @Test
    void holidaysBetween_dedupesOverlappingDoubleDateWithMajorEvent() {
        var hits = DssHolidayCalendar.holidaysBetween(
            LocalDate.of(2025, 12, 20),
            LocalDate.of(2026, 1, 5)
        );
        assertTrue(hits.stream().anyMatch(h -> h.code().equals("XMAS")));
        assertFalse(hits.stream().anyMatch(h -> h.code().equals("DOUBLE_11")));
    }

    @Test
    void forecastScopeLabel_mentionsEventCount() {
        String label = DssHolidayCalendar.forecastScopeLabel(
            LocalDate.of(2025, 11, 10),
            LocalDate.of(2025, 11, 24),
            2
        );
        assertTrue(label.contains("Phạm vi dự báo"));
        assertTrue(label.contains("2 sự kiện"));
    }
}
