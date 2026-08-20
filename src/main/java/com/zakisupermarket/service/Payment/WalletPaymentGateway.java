package com.zakisupermarket.service.Payment;

import com.zakisupermarket.dto.request.PaymentRequest;
import com.zakisupermarket.dto.response.PaymentResponse;
import com.zakisupermarket.entity.Payment;
import com.zakisupermarket.entity.enums.PaymentMethod;
import com.zakisupermarket.repository.PaymentRepository;
import com.zakisupermarket.repository.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletPaymentGateway extends BasePaymentGateway {

    public WalletPaymentGateway(PaymentRepository paymentRepository, StoreRepository storeRepository) {
        super(paymentRepository, storeRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.WALLET;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return false;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing Wallet payment: {}", referenceNumber);

        try {
            Thread.sleep(500);

            return PaymentResponse.builder()
                    .status("COMPLETED")
                    .message("Payment processed successfully via Wallet")
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.WALLET.name())
                    .amount(request.getAmount())
                    .transactionId("WALLET-" + System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Wallet payment failed: {}", e.getMessage());
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Wallet payment failed: " + e.getMessage())
                    .referenceNumber(referenceNumber)
                    .paymentMethod(PaymentMethod.WALLET.name())
                    .amount(request.getAmount())
                    .build();
        }
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, java.math.BigDecimal amount, String reason) {
        log.info("Processing Wallet refund: {}, amount: {}", payment.getReferenceNumber(), amount);

        return PaymentResponse.builder()
                .status("COMPLETED")
                .message("Wallet refund processed successfully")
                .referenceNumber(payment.getReferenceNumber())
                .paymentMethod(PaymentMethod.WALLET.name())
                .amount(amount)
                .transactionId("REFUND-WALLET-" + System.currentTimeMillis())
                .build();
    }
}