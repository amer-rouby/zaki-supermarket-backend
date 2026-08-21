package com.zakisupermarket.dto.response;

import com.zakisupermarket.entity.CustomerTransaction;
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
public class CustomerTransactionResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private Long relatedSaleId;
    private BigDecimal balanceAfter;
    private String createdByName;
    private String notes;
    private LocalDateTime createdAt;

    public static CustomerTransactionResponse fromEntity(CustomerTransaction t) {
        return CustomerTransactionResponse.builder()
                .id(t.getId())
                .type(t.getType().name())
                .amount(t.getAmount())
                .relatedSaleId(t.getRelatedSaleId())
                .balanceAfter(t.getBalanceAfter())
                .createdByName(t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : null)
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
