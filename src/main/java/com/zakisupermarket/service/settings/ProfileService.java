package com.zakisupermarket.service.settings;

import com.zakisupermarket.dto.settings.request.ProfileUpdateRequest;
import com.zakisupermarket.dto.settings.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile(Long userId);
    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);
    ProfileResponse changePassword(Long userId, String oldPassword, String newPassword);

    void updateProfileImageUrl(Long userId, String imageUrl);
}