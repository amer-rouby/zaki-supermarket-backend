package com.zakisupermarket.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String referenceNumber;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String message;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // Set only when status is "FAILED" - a gateway failure is returned as a normal
    // 200 response (see BasePaymentGateway.createErrorResponse), never an exception,
    // so it needs its own errorCode/params carried here instead of on ApiResponse -
    // the frontend resolves `code` to ERRORS.<code> exactly like an HTTP error.
    private String code;
    private Map<String, Object> params;
}