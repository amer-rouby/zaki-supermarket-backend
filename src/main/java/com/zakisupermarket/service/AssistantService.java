package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.AssistantAnswer;

public interface AssistantService {
    AssistantAnswer ask(String query, Long storeId);
}
