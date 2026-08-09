package com.example.secdsp.modules.user.service;

import com.example.secdsp.modules.user.dto.request.UpdateSellerMomoRequest;
import com.example.secdsp.modules.user.dto.response.SellerMomoPublicResponse;
import com.example.secdsp.modules.user.dto.response.SellerMomoSettingsResponse;

public interface SellerMomoService {

    SellerMomoSettingsResponse getMyMomoSettings();

    SellerMomoSettingsResponse updateMyMomoSettings(UpdateSellerMomoRequest request);

    SellerMomoPublicResponse getPublicMomo(Long sellerId);
}
