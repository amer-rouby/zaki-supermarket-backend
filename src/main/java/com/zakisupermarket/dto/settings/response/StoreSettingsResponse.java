package com.zakisupermarket.dto.settings.response;

import com.zakisupermarket.entity.settings.StoreSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettingsResponse {

    private Long id;
    private Long storeId;
    private String storeName;
    private String address;
    private String phone;
    private String email;
    private String licenseNumber;
    private String taxNumber;
    private String commercialRegister;
    private String logoUrl;
    private String currency;
    private String timezone;
    private String dateFormat;
    private String timeFormat;
    private String enabledPaymentMethods;
    private BigDecimal largeSaleThreshold;
    private BigDecimal largeExpenseThreshold;
    private LocalDateTime updatedAt;

    public static StoreSettingsResponse fromEntity(StoreSettings settings, String storeName) {
        return StoreSettingsResponse.builder()
                .id(settings.getId())
                .storeId(settings.getStore().getId())
                .storeName(storeName)
                .address(settings.getAddress())
                .phone(settings.getPhone())
                .email(settings.getEmail())
                .licenseNumber(settings.getLicenseNumber())
                .taxNumber(settings.getTaxNumber())
                .commercialRegister(settings.getCommercialRegister())
                .logoUrl(settings.getLogoUrl())
                .currency(settings.getCurrency())
                .timezone(settings.getTimezone())
                .dateFormat(settings.getDateFormat())
                .timeFormat(settings.getTimeFormat())
                .enabledPaymentMethods(settings.getEnabledPaymentMethods())
                .largeSaleThreshold(settings.getLargeSaleThreshold())
                .largeExpenseThreshold(settings.getLargeExpenseThreshold())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}