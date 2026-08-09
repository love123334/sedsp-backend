package com.example.secdsp.modules.user.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.user.dto.request.UpdateSellerMomoRequest;
import com.example.secdsp.modules.user.dto.response.SellerMomoPublicResponse;
import com.example.secdsp.modules.user.dto.response.SellerMomoSettingsResponse;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SellerMomoServiceImpl implements SellerMomoService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public SellerMomoSettingsResponse getMyMomoSettings() {
        User seller = requireSeller(requireUserId());
        return toSettings(seller);
    }

    @Override
    @Transactional
    public SellerMomoSettingsResponse updateMyMomoSettings(UpdateSellerMomoRequest request) {
        User seller = requireSeller(requireUserId());
        if (request.getMomoPhone() != null) {
            seller.setMomoPhone(normalizePhone(request.getMomoPhone()));
        }
        if (request.getMomoQrUrl() != null) {
            seller.setMomoQrUrl(normalizeUrl(request.getMomoQrUrl()));
        }
        if (!isConfigured(seller)) {
            throw new BusinessException(
                "Cần ít nhất số MoMo hoặc ảnh QR.",
                HttpStatus.BAD_REQUEST
            );
        }
        return toSettings(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerMomoPublicResponse getPublicMomo(Long sellerId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));
        return SellerMomoPublicResponse.builder()
            .sellerId(seller.getId())
            .storeName(seller.getStoreName())
            .momoPhone(seller.getMomoPhone())
            .momoQrUrl(seller.getMomoQrUrl())
            .configured(isConfigured(seller))
            .build();
    }

    public static boolean isConfigured(User seller) {
        return StringUtils.hasText(seller.getMomoPhone())
            || StringUtils.hasText(seller.getMomoQrUrl());
    }

    static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed.replaceAll("\\s+", "");
    }

    private static String normalizeUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SellerMomoSettingsResponse toSettings(User seller) {
        return SellerMomoSettingsResponse.builder()
            .momoPhone(seller.getMomoPhone())
            .momoQrUrl(seller.getMomoQrUrl())
            .configured(isConfigured(seller))
            .build();
    }

    private User requireSeller(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!UserRole.SELLER.name().equals(user.getRole().getName())) {
            throw new UnauthorizedException("Seller role required.");
        }
        return user;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
