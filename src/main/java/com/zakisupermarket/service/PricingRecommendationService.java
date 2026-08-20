package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.PricingRecommendationDTO;

import java.util.List;

public interface PricingRecommendationService {
    List<PricingRecommendationDTO> getRecommendations(Long storeId);
}
