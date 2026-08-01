package com.example.secdsp.modules.platformrevenue.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlatformRevenueDashboardRequest {

    @NotNull(message = "From date is required")
    @PastOrPresent(message = "From date must not be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fromDate;

    @NotNull(message = "To date is required")
    @PastOrPresent(message = "To date must not be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate toDate;

    @NotNull(message = "Granularity is required")
    RevenueGranularity granularity = RevenueGranularity.DAY;

    @NotNull(message = "Top limit is required")
    @Min(value = 1, message = "Top limit must be at least 1")
    @Max(value = 20, message = "Top limit must not exceed 20")
    Integer topLimit = 5;

    @AssertTrue(message = "From date must not be after to date")
    public boolean isDateRangeValid() {
        return fromDate == null
            || toDate == null
            || !fromDate.isAfter(toDate);
    }
}
