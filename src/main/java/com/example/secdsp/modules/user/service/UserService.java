package com.example.secdsp.modules.user.service;

import com.example.secdsp.modules.user.dto.request.UpdateProfileRequest;
import com.example.secdsp.modules.user.dto.response.UserProfileResponse;
import com.example.secdsp.modules.user.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    Page<UserSummaryResponse> getUsers(String keyword, Pageable pageable);

    UserProfileResponse getUserById(Long id);
}
