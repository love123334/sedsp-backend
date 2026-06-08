package com.example.secdsp.modules.auth.service;

import com.example.secdsp.modules.auth.dto.request.LoginRequest;
import com.example.secdsp.modules.auth.dto.response.LoginResponse;
import com.example.secdsp.modules.auth.dto.response.MeResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    MeResponse getCurrentUser();
}
