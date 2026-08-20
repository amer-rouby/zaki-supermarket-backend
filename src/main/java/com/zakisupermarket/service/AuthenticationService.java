package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.LoginRequest;
import com.zakisupermarket.dto.request.RegisterRequest;
import com.zakisupermarket.dto.response.AuthResponse;

public interface AuthenticationService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /** Second step of login when the first step returned twoFactorRequired=true. */
    AuthResponse completeTwoFactorLogin(String tempToken, String code);

    AuthResponse refreshToken(String refreshToken);

    void logout(String accessToken);
}