package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.response.AssistantAnswer;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.AssistantProvider;
import com.zakisupermarket.service.AssistantService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final AssistantProvider assistantProvider;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    @Override
    @Transactional(readOnly = true)
    public AssistantAnswer ask(String query, Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getAiAssistantEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("Zaki assistant feature is disabled for this store");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new RuntimeException("Question cannot be empty");
        }
        return assistantProvider.answer(query, storeId);
    }
}
