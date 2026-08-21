package com.zakisupermarket.dto.response;

import com.zakisupermarket.entity.Customer;
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
public class CustomerResponse {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private BigDecimal availableCredit;
    private String status;
    private String notes;
    private Long storeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse fromEntity(Customer c) {
        BigDecimal creditLimit = c.getCreditLimit() != null ? c.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal currentBalance = c.getCurrentBalance() != null ? c.getCurrentBalance() : BigDecimal.ZERO;
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .creditLimit(creditLimit)
                .currentBalance(currentBalance)
                .availableCredit(creditLimit.subtract(currentBalance).max(BigDecimal.ZERO))
                .status(c.getStatus())
                .notes(c.getNotes())
                .storeId(c.getStore() != null ? c.getStore().getId() : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
