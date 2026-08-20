package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboardStats(Long storeId);
}