package com.zakisupermarket.service.Payment;

import com.zakisupermarket.dto.request.PaymentRequest;
import com.zakisupermarket.dto.response.PaymentResponse;
import com.zakisupermarket.entity.Payment;
import com.zakisupermarket.entity.enums.PaymentMethod;
import com.zakisupermarket.repository.PaymentRepository;
import com.zakisupermarket.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class BankTransferPaymentGateway extends BasePaymentGateway {

    public BankTransferPaymentGateway(PaymentRepository paymentRepository, StoreRepository storeRepository) {
        super(paymentRepository, storeRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return false;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing Bank Transfer payment: {}", referenceNumber);

        // Simulate Bank Transfer gateway
        // In production, this would call the Bank API or generate transfer account details
        try {
            Thread.sleep(1000); // Simulate network delay

            // Bank transfers are typically pending until the user completes the transfer
            return PaymentResponse.builder()
                    .status("PENDING")
                    .message("Bank transfer initiated. Please complete the transfer to the provided account details.")
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER.name())
                    .amount(request.getAmount())
                    .transactionId("BANK-" + System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Bank Transfer payment failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Bank transfer failed: " + e.getMessage())
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER.name())
                    .amount(request.getAmount())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        log.info("Processing Bank Transfer refund: {}, amount: {}", payment.getReferenceNumber(), amount);

        // Bank transfers refunds usually take time
        return PaymentResponse.builder()
                .status("PENDING")
                .message("Bank transfer refund initiated. Processing time: 3-5 business days.")
                .referenceNumber(payment.getReferenceNumber())
                .paymentMethod(PaymentMethod.BANK_TRANSFER.name())
                .amount(amount)
                .transactionId("REFUND-BANK-" + System.currentTimeMillis())
                .build();
    }
}