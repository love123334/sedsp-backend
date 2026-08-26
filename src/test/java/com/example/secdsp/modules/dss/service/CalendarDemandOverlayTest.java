package com.example.secdsp.modules.dss.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarDemandOverlayTest {

    @Test
    void replaysDoubleDayLiftOntoMatchingForecastDates() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        List<Long> history = new ArrayList<>();
        LocalDate date = start;
        for (int i = 0; i < 180; i++) {
            long qty = date.getDayOfMonth() == date.getMonthValue() ? 7L : 6L;
            history.add(qty);
            date = date.plusDays(1);
        }

        CalendarDemandOverlay.Lifts lifts = CalendarDemandOverlay.estimate(start, history);
        assertTrue(lifts.doubleDay() >= 0.5, "double-day lift should be visible: " + lifts);

        assertTrue(
            CalendarDemandOverlay.liftOn(LocalDate.of(2026, 9, 9), lifts) >= 0.5,
            "9/9 must keep the historical spike"
        );
        assertEquals(
            0.0,
            CalendarDemandOverlay.liftOn(LocalDate.of(2026, 9, 10), lifts),
            0.0001
        );
    }
}
