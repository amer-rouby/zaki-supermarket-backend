package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.SendWhatsAppResponse;

public interface WhatsAppClientService {
    SendWhatsAppResponse sendMessage(String phoneNumber, String message);
}