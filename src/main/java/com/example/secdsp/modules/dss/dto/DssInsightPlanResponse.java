package com.example.secdsp.modules.dss.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssInsightPlanResponse {

    String source;
    String commentary;
    Object metrics;
    String powerBiEmbedUrl;
    String powerBiReportTitle;
    String powerBiFeedHint;
    String generatedAt;
}
