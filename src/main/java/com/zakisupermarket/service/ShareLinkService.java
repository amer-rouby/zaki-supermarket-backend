package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.CreateShareLinkRequest;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import com.zakisupermarket.entity.ShareLink;

public interface ShareLinkService {

    ShareLinkResponse createShareLink(CreateShareLinkRequest request, Long createdBy, Long storeId);

    ShareLink validateShareLink(String token);

    void incrementAccessCount(String token);

    void cleanupExpiredLinks();

    String buildShareUrl(ShareLink shareLink);
}