package com.example.secdsp.common.util;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTimeTest {

    @Test
    void orderJustAfterVnMidnightStaysOnVnCalendarDay() {
        OffsetDateTime createdUtc = OffsetDateTime.parse("2026-08-26T17:13:37Z");
        assertEquals(LocalDate.of(2026, 8, 27), AppTime.toAppDate(createdUtc));

        LocalDateTime storedByHibernateJdbcVn =
            createdUtc.atZoneSameInstant(AppTime.ZONE).toLocalDateTime();
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 13, 37), storedByHibernateJdbcVn);
        assertEquals(LocalDate.of(2026, 8, 27), AppTime.toAppDate(storedByHibernateJdbcVn));
        assertEquals(LocalDate.of(2026, 8, 27), AppTime.toAppDate(Date.valueOf("2026-08-27")));
        assertEquals(LocalDate.of(2026, 8, 27), AppTime.toAppDate("2026-08-27T00:13:37"));
    }

    @Test
    void timezoneThenUtcDateCastIsTheOldChartBug() {
        LocalDateTime storedVnWall = LocalDateTime.of(2026, 8, 27, 0, 13, 37);
        LocalDate utcDateAfterTimezoneFn =
            storedVnWall.atZone(AppTime.ZONE).toLocalDateTime()
                .atZone(AppTime.ZONE)
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        assertEquals(
            LocalDate.of(2026, 8, 26),
            utcDateAfterTimezoneFn,
            "timezone(VN, naive-VN)::date with session TZ=UTC shifts 00:00-06:59 VN back one day"
        );
    }
}
