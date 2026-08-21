package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.DashboardResponse;
import com.zakisupermarket.dto.response.ZakiInsightsDTO;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long storeId);

    ZakiInsightsDTO getZakiInsights(Long storeId);
}