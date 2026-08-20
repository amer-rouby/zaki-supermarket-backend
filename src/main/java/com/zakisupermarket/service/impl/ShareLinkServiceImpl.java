package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.CreateShareLinkRequest;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import com.zakisupermarket.entity.ShareLink;
import com.zakisupermarket.repository.ShareLinkRepository;
import com.zakisupermarket.service.ShareLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkServiceImpl implements ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;

    @Value("${app.base-url:https://zakisupermarket.app}")
    private String baseUrl;

    @Override
    @Transactional
    public ShareLinkResponse createShareLink(CreateShareLinkRequest request, Long createdBy, Long storeId) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(request.getExpiryHours());

        ShareLink shareLink = ShareLink.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .storeId(storeId)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .isActive(true)
                .accessCount(0)
                .build();

        shareLink.generateToken();

        ShareLink saved = shareLinkRepository.save(shareLink);

        String shareUrl = buildShareUrl(saved);

        log.info("Share link created: token={}, entityType={}, entityId={}, expiresAt={}",
                saved.getToken(), saved.getEntityType(), saved.getEntityId(), saved.getExpiresAt());

        return ShareLinkResponse.builder()
                .shareUrl(shareUrl)
                .token(saved.getToken())
                .expiresAt(saved.getExpiresAt())
                .entityType(saved.getEntityType())
                .entityId(saved.getEntityId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShareLink validateShareLink(String token) {
        return shareLinkRepository.findActiveByToken(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired share link"));
    }

    @Override
    @Transactional
    public void incrementAccessCount(String token) {
        shareLinkRepository.incrementAccessCount(token);
    }

    @Override
    public String buildShareUrl(ShareLink shareLink) {
        return String.format("%s/share/%s/%s",
                baseUrl,
                shareLink.getEntityType().toLowerCase(),
                shareLink.getToken());
    }

    // Clean up expired share links every hour
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredLinks() {
        shareLinkRepository.deactivateExpiredLinks(LocalDateTime.now());
        log.info("Expired share links cleaned up");
    }
}