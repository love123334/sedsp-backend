package com.example.secdsp.modules.dss.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Replays calendar spikes that actually appear in this product's history
 * (ngày đôi, ngày lương) onto the forecast horizon. Holt/MA flatten
 * those rare jumps; this overlay puts them back when the lift is real.
 */
public final class CalendarDemandOverlay {

    public static final String DOUBLE = "DOUBLE";
    public static final String PAYDAY = "PAYDAY";
    public static final String BASE = "BASE";

    private static final double MIN_LIFT = 0.5;
    private static final int MIN_SAMPLES = 2;

    public record Lifts(double doubleDay, double payday) {
        public static Lifts none() {
            return new Lifts(0.0, 0.0);
        }
    }

    private CalendarDemandOverlay() {
    }

    public static Lifts estimate(LocalDate historyStart, List<Long> history) {
        if (historyStart == null || history == null || history.size() < 14) {
            return Lifts.none();
        }

        List<Long> base = new ArrayList<>();
        List<Long> doubles = new ArrayList<>();
        List<Long> paydays = new ArrayList<>();

        LocalDate date = historyStart;
        for (long qty : history) {
            switch (tag(date)) {
                case DOUBLE -> doubles.add(qty);
                case PAYDAY -> paydays.add(qty);
                default -> base.add(qty);
            }
            date = date.plusDays(1);
        }

        double baseline = mean(base);
        return new Lifts(
            lift(doubles, baseline),
            lift(paydays, baseline)
        );
    }

    public static double liftOn(LocalDate date, Lifts lifts) {
        if (date == null || lifts == null) {
            return 0.0;
        }
        return switch (tag(date)) {
            case DOUBLE -> lifts.doubleDay();
            case PAYDAY -> lifts.payday();
            default -> 0.0;
        };
    }

    static String tag(LocalDate date) {
        if (date.getDayOfMonth() == date.getMonthValue()) {
            return DOUBLE;
        }
        int day = date.getDayOfMonth();
        if (day == 15 || day == 25) {
            return PAYDAY;
        }
        return BASE;
    }

    private static double lift(List<Long> samples, double baseline) {
        if (samples.size() < MIN_SAMPLES) {
            return 0.0;
        }
        double value = mean(samples) - baseline;
        return value >= MIN_LIFT ? value : 0.0;
    }

    private static double mean(List<Long> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return sum / (double) values.size();
    }
}
