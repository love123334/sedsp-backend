package com.example.secdsp.modules.dss.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lịch sự kiện bán lẻ VN — hệ số nhu cầu & gợi ý áp lực giá cho kỳ dự báo.
 * Không thay thế dữ liệu bán thực tế; chỉ điều chỉnh kỳ dự báo tương lai.
 */
public final class DssHolidayCalendar {

    private static final DateTimeFormatter VI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record HolidayWindow(
        String code,
        String label,
        LocalDate start,
        LocalDate end,
        BigDecimal demandMultiplier,
        String note,
        String priceImpactNote
    ) {}

    private static final List<HolidayWindow> WINDOWS = buildWindows();

    private DssHolidayCalendar() {
    }

    public static List<HolidayWindow> holidaysBetween(LocalDate from, LocalDate to) {
        List<HolidayWindow> hits = new ArrayList<>();
        for (HolidayWindow w : WINDOWS) {
            if (!w.end().isBefore(from) && !w.start().isAfter(to)) {
                hits.add(w);
            }
        }
        return dedupeForDisplay(hits);
    }

    /** Sự kiện áp dụng cho một ngày (ưu tiên sự kiện lớn — thứ tự trong WINDOWS). */
    public static HolidayWindow holidayOn(LocalDate date) {
        for (HolidayWindow w : WINDOWS) {
            if (!date.isBefore(w.start()) && !date.isAfter(w.end())) {
                return w;
            }
        }
        return null;
    }

    public static String forecastScopeLabel(LocalDate from, LocalDate to, int eventCount) {
        String range = formatVi(from) + " → " + formatVi(to);
        if (eventCount <= 0) {
            return "Phạm vi dự báo: " + range
                + " — không có ngày lễ / mega sale lớn trong kỳ (chủ yếu mùa vụ theo thứ).";
        }
        return "Phạm vi dự báo: " + range
            + " — có " + eventCount + " sự kiện ảnh hưởng nhu cầu & áp lực giá.";
    }

    private static List<HolidayWindow> buildWindows() {
        List<HolidayWindow> all = new ArrayList<>();

        // Sự kiện lớn — đặt trước để holidayOn() ưu tiên
        all.addAll(List.of(
            window("TET", "Tết Nguyên Đán", "2025-01-25", "2025-02-05", "1.45",
                "Mua sắm Tết — thời trang, quà tặng, điện tử thường tăng.",
                "Tránh tăng giá; ưu tiên tồn kho, giao nhanh — khách nhạy khuyến mãi & quà tặng."),
            window("TET", "Tết Nguyên Đán", "2026-02-14", "2026-02-23", "1.45",
                "Mua sắm Tết — khuyến mãi và nhu cầu cao điểm.",
                "Cạnh tranh khuyến mãi cao; bundle / quà kèm hiệu quả hơn tăng giá."),
            window("TET", "Tết Nguyên Đán", "2027-02-05", "2027-02-14", "1.45",
                "Mua sắm Tết.",
                "Giữ giá ổn định, tập trung flash sale ngắn và freeship."),
            window("VALENTINE", "Valentine 14/2", "2025-02-12", "2025-02-14", "1.12",
                "Quà tặng, mỹ phẩm, phụ kiện tăng nhẹ.",
                "Combo quà / giảm nhẹ 3–8% — khách so sánh giá quà tặng."),
            window("VALENTINE", "Valentine 14/2", "2026-02-12", "2026-02-14", "1.12", "", ""),
            window("VALENTINE", "Valentine 14/2", "2027-02-12", "2027-02-14", "1.12", "", ""),
            window("WOMEN_83", "Quốc tế Phụ nữ 8/3", "2025-03-06", "2025-03-09", "1.18",
                "Thời trang, mỹ phẩm, quà tặng thường tăng.",
                "Khuyến mãi theo combo / voucher — áp lực giảm giá từ đối thủ cùng ngành."),
            window("WOMEN_83", "Quốc tế Phụ nữ 8/3", "2026-03-06", "2026-03-09", "1.18", "", ""),
            window("WOMEN_83", "Quốc tế Phụ nữ 8/3", "2027-03-06", "2027-03-09", "1.18", "", ""),
            window("LABOR", "30/4 – 1/5", "2025-04-28", "2025-05-02", "1.10",
                "Nghỉ lễ — du lịch, điện tử, phụ kiện tăng vừa.",
                "Sàn thường sale ngắn; cân nhắc voucher thay vì hạ giá sâu toàn shop."),
            window("LABOR", "30/4 – 1/5", "2026-04-28", "2026-05-02", "1.10", "", ""),
            window("LABOR", "30/4 – 1/5", "2027-04-28", "2027-05-02", "1.10", "", ""),
            window("MID_618", "618 — Sale giữa năm", "2025-06-16", "2025-06-19", "1.20",
                "Mega sale giữa năm (Shopee/Lazada) — điện tử, gia dụng.",
                "Cạnh tranh giá mạnh; flash sale / giảm có trần để giữ biên lợi nhuận."),
            window("MID_618", "618 — Sale giữa năm", "2026-06-16", "2026-06-19", "1.20", "", ""),
            window("MID_618", "618 — Sale giữa năm", "2027-06-16", "2027-06-19", "1.20", "", ""),
            window("BACK_TO_SCHOOL", "Mùa tựu trường", "2025-08-10", "2025-08-25", "1.14",
                "Sách vở, balo, điện tử học tập, phụ kiện.",
                "Khuyến mãi bundle — khách so sánh giá theo combo học sinh."),
            window("BACK_TO_SCHOOL", "Mùa tựu trường", "2026-08-10", "2026-08-25", "1.14", "", ""),
            window("BACK_TO_SCHOOL", "Mùa tựu trường", "2027-08-10", "2027-08-25", "1.14", "", ""),
            window("NATIONAL", "Quốc khánh 2/9", "2025-08-30", "2025-09-03", "1.10",
                "Nghỉ lễ dài — mua sắm online tăng vừa.",
                "Sale ngắn trên sàn; tránh tăng giá khi đối thủ giảm."),
            window("NATIONAL", "Quốc khánh 2/9", "2026-08-30", "2026-09-03", "1.10", "", ""),
            window("NATIONAL", "Quốc khánh 2/9", "2027-08-30", "2027-09-03", "1.10", "", ""),
            window("SALE_1010", "10.10 — Sale tháng 10", "2025-10-08", "2025-10-11", "1.22",
                "Warm-up trước 11.11 — voucher & flash sale.",
                "Giảm có kiểm soát 5–12% hoặc quà kèm — chuẩn bị tồn cho 11.11."),
            window("SALE_1010", "10.10 — Sale tháng 10", "2026-10-08", "2026-10-11", "1.22", "", ""),
            window("SALE_1010", "10.10 — Sale tháng 10", "2027-10-08", "2027-10-11", "1.22", "", ""),
            window("WOMEN_2010", "Phụ nữ Việt Nam 20/10", "2025-10-18", "2025-10-21", "1.15",
                "Khuyến mãi ngành thời trang / quà tặng.",
                "Ưu đãi theo ngành — tránh tăng giá khi đối thủ sale sâu."),
            window("WOMEN_2010", "Phụ nữ Việt Nam 20/10", "2026-10-18", "2026-10-21", "1.15", "", ""),
            window("WOMEN_2010", "Phụ nữ Việt Nam 20/10", "2027-10-18", "2027-10-21", "1.15", "", ""),
            window("SINGLE_11", "11.11 — Mega sale", "2025-11-09", "2025-11-12", "1.35",
                "Flash sale toàn sàn — giá & voucher ảnh hưởng mạnh doanh số.",
                "Áp lực giảm giá cao nhất năm — cần kịch bản giá & tồn kho riêng cho 11.11."),
            window("SINGLE_11", "11.11 — Mega sale", "2026-11-09", "2026-11-12", "1.35", "", ""),
            window("SINGLE_11", "11.11 — Mega sale", "2027-11-09", "2027-11-12", "1.35", "", ""),
            window("BLACK_FRIDAY", "Black Friday", "2025-11-27", "2025-11-30", "1.22",
                "Giảm giá điện tử / phụ kiện — sau 11.11.",
                "Cạnh tranh giá tiếp diễn; giảm 5–15% hoặc voucher để giữ conversion."),
            window("BLACK_FRIDAY", "Black Friday", "2026-11-26", "2026-11-29", "1.22", "", ""),
            window("BLACK_FRIDAY", "Black Friday", "2027-11-26", "2027-11-29", "1.22", "", ""),
            window("DOUBLE_12", "12.12 — Sale cuối năm", "2025-12-10", "2025-12-13", "1.30",
                "Đẩy doanh số cuối năm, cạnh tranh giá.",
                "Clearance cuối năm — cân nhắc giảm sâu hơn để xả tồn."),
            window("DOUBLE_12", "12.12 — Sale cuối năm", "2026-12-10", "2026-12-13", "1.30", "", ""),
            window("DOUBLE_12", "12.12 — Sale cuối năm", "2027-12-10", "2027-12-13", "1.30", "", ""),
            window("XMAS", "Giáng sinh & Năm mới", "2025-12-20", "2026-01-02", "1.12",
                "Quà tặng, trang trí, thời trang đông.",
                "Quà tặng / combo — khách nhạy ưu đãi cuối năm, logistics có thể chậm."),
            window("XMAS", "Giáng sinh & Năm mới", "2026-12-20", "2027-01-02", "1.12", "", ""),
            window("XMAS", "Giáng sinh & Năm mới", "2027-12-20", "2028-01-02", "1.12", "", "")
        ));

        for (int year : new int[] {2025, 2026, 2027}) {
            all.addAll(doubleDateWindows(year));
        }

        return List.copyOf(all);
    }

    /** Ngày đôi 1/1, 2/2, … 9/9 — mini sale phổ biến trên TMĐT VN. */
    private static List<HolidayWindow> doubleDateWindows(int year) {
        List<HolidayWindow> list = new ArrayList<>();
        for (int month = 1; month <= 9; month++) {
            LocalDate center = LocalDate.of(year, month, month);
            String mult = month <= 4 ? "1.08" : month <= 7 ? "1.10" : "1.12";
            list.add(window(
                "DOUBLE_" + month + month,
                String.format("Ngày đôi %d/%d — mini sale", month, month),
                center.minusDays(1).toString(),
                center.plusDays(1).toString(),
                mult,
                "Sàn thường flash sale / voucher ngắn — nhu cầu tăng nhẹ đến vừa.",
                "Áp lực giảm giá từ voucher sàn — nên khớp ưu đãi hoặc combo thay vì giữ giá cứng."
            ));
        }
        return list;
    }

    /** Gộp sự kiện trùng kỳ — giữ sự kiện có hệ số nhu cầu cao hơn. */
    private static List<HolidayWindow> dedupeForDisplay(List<HolidayWindow> hits) {
        List<HolidayWindow> sorted = new ArrayList<>(hits);
        sorted.sort(Comparator.comparing(HolidayWindow::demandMultiplier).reversed());
        List<HolidayWindow> kept = new ArrayList<>();
        for (HolidayWindow candidate : sorted) {
            boolean overlapsKept = kept.stream().anyMatch(k -> overlaps(k, candidate));
            if (!overlapsKept) {
                kept.add(candidate);
            }
        }
        kept.sort(Comparator.comparing(HolidayWindow::start));
        return kept;
    }

    private static boolean overlaps(HolidayWindow a, HolidayWindow b) {
        return !a.end().isBefore(b.start()) && !b.end().isBefore(a.start());
    }

    private static String formatVi(LocalDate date) {
        return date.format(VI_DATE);
    }

    private static HolidayWindow window(
        String code,
        String label,
        String start,
        String end,
        String multiplier,
        String note,
        String priceImpactNote
    ) {
        return new HolidayWindow(
            code,
            label,
            LocalDate.parse(start),
            LocalDate.parse(end),
            new BigDecimal(multiplier),
            note,
            priceImpactNote
        );
    }
}
