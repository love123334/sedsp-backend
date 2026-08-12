package com.example.secdsp.modules.dss.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DssAiInsightResponse {

    String title;

    String summary;

    String provider;

    boolean fallback;

    String disclaimer;
}
