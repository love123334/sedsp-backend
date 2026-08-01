package com.example.secdsp.modules.platformrevenue.service;

import com.example.secdsp.modules.platformrevenue.dto.request.PlatformRevenueDashboardRequest;
import com.example.secdsp.modules.platformrevenue.dto.response.PlatformRevenueDashboardResponse;

public interface PlatformRevenueService {

    PlatformRevenueDashboardResponse getDashboard(
        PlatformRevenueDashboardRequest request
    );
}
