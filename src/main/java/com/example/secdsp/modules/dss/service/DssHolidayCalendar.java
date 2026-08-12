package com.example.secdsp.modules.dss.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lịch sự kiện bán lẻ VN (cố định theo năm) — hệ số nhu cầu ước lượng cho dự báo.
 * Không thay thế dữ liệu bán thực tế; chỉ điều chỉnh kỳ dự báo tương lai.
 */
public final class DssHolidayCalendar {

    public record HolidayWindow(
        String code,
        String label,
        LocalDate start,
        LocalDate end,
        BigDecimal demandMultiplier,
        String note
    ) {}

    private static final List<HolidayWindow> WINDOWS = List.of(
        window("TET", "Tết Nguyên Đán", "2025-01-25", "2025-02-05", "1.45",
            "Mua sắm Tết — thời trang, quà tặng, điện tử thường tăng."),
        window("TET", "Tết Nguyên Đán", "2026-02-14", "2026-02-23", "1.45",
            "Mua sắm Tết — khuyến mãi và nhu cầu cao điểm."),
        window("TET", "Tết Nguyên Đán", "2027-02-05", "2027-02-14", "1.45",
            "Mua sắm Tết."),
        window("WOMEN_83", "Quốc tế Phụ nữ 8/3", "2026-03-06", "2026-03-09", "1.18",
            "Thời trang, mỹ phẩm, quà tặng thường tăng."),
        window("WOMEN_83", "Quốc tế Phụ nữ 8/3", "2025-03-06", "2025-03-09", "1.18", ""),
        window("WOMEN_2010", "Phụ nữ Việt Nam 20/10", "2026-10-18", "2026-10-21", "1.15",
            "Khuyến mãi ngành thời trang / quà tặng."),
        window("WOMEN_2010", "Phụ nữ Việt Nam 20/10", "2025-10-18", "2025-10-21", "1.15", ""),
        window("SINGLE_11", "11.11 — Sale lớn", "2025-11-09", "2025-11-12", "1.35",
            "Flash sale sàn — giá và khuyến mãi ảnh hưởng mạnh doanh số."),
        window("SINGLE_11", "11.11 — Sale lớn", "2026-11-09", "2026-11-12", "1.35", ""),
        window("DOUBLE_12", "12.12 — Sale cuối năm", "2025-12-10", "2025-12-13", "1.30",
            "Đẩy doanh số cuối năm, cạnh tranh giá."),
        window("DOUBLE_12", "12.12 — Sale cuối năm", "2026-12-10", "2026-12-13", "1.30", ""),
        window("BLACK_FRIDAY", "Black Friday", "2025-11-27", "2025-11-30", "1.22",
            "Giảm giá điện tử / phụ kiện."),
        window("BLACK_FRIDAY", "Black Friday", "2026-11-26", "2026-11-29", "1.22", ""),
        window("XMAS", "Giáng sinh & Năm mới", "2025-12-20", "2026-01-02", "1.12",
            "Quà tặng, trang trí, thời trong đông."),
        window("XMAS", "Giáng sinh & Năm mới", "2026-12-20", "2027-01-02", "1.12", "")
    );

    private DssHolidayCalendar() {
    }

    public static List<HolidayWindow> holidaysBetween(LocalDate from, LocalDate to) {
        List<HolidayWindow> hits = new ArrayList<>();
        for (HolidayWindow w : WINDOWS) {
            if (!w.end().isBefore(from) && !w.start().isAfter(to)) {
                hits.add(w);
            }
        }
        return hits;
    }

    public static HolidayWindow holidayOn(LocalDate date) {
        for (HolidayWindow w : WINDOWS) {
            if (!date.isBefore(w.start()) && !date.isAfter(w.end())) {
                return w;
            }
        }
        return null;
    }

    private static HolidayWindow window(
        String code,
        String label,
        String start,
        String end,
        String multiplier,
        String note
    ) {
        return new HolidayWindow(
            code,
            label,
            LocalDate.parse(start),
            LocalDate.parse(end),
            new BigDecimal(multiplier),
            note
        );
    }
}
