package com.zakisupermarket.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotEmpty(message = "At least one item is required")
    private List<SaleItemRequest> items;

    private String notes;

    private String customerPhone;

    // Only required/used when paymentMethod is CREDIT - identifies which
    // customer account to charge against their credit limit.
    private Long customerId;

    private String paymentMethod = "CASH";

    // Set only when this request is a sync of a sale queued while offline -
    // lets the server recognize a retried sync as the same sale rather than
    // creating a duplicate. Null for a normal online sale.
    private String clientReferenceId;

    @DecimalMin(value = "0", message = "Discount cannot be negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be positive")
    private BigDecimal totalAmount;
}