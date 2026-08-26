package com.example.secdsp.common.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Business calendar for SEDSP. Orders and DSS charts bucket by
 * {@code Asia/Ho_Chi_Minh} dates, not the JVM or Postgres session TZ.
 *
 * {@code orders.created_at} is TIMESTAMP WITHOUT TIME ZONE written as Vietnam
 * wall clock (Hibernate {@code jdbc.time_zone=Asia/Ho_Chi_Minh}). Extract the
 * calendar date from that stored local datetime — do not run
 * {@code timezone('Asia/Ho_Chi_Minh', created_at)::date} when Postgres
 * {@code TimeZone} is UTC: instants 00:00–06:59 VN become the previous UTC day.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AppTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDate toAppDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.atZoneSameInstant(ZONE).toLocalDate();
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.withZoneSameInstant(ZONE).toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZONE).toLocalDate();
        }
        if (value instanceof String raw) {
            String trimmed = raw.trim();
            if (trimmed.length() >= 10) {
                return LocalDate.parse(trimmed.substring(0, 10));
            }
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZONE).toLocalDate();
        }
        throw new IllegalArgumentException("Cannot parse app calendar date from " + value.getClass());
    }
}
