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
public class CashPaymentGateway extends BasePaymentGateway {

    public CashPaymentGateway(PaymentRepository paymentRepository, StoreRepository storeRepository) {
        super(paymentRepository, storeRepository);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CASH;
    }

    @Override
    public boolean isSupported(PaymentMethod method) {
        return false;
    }

    @Override
    protected PaymentResponse callGatewayAPI(PaymentRequest request, String referenceNumber) {
        log.info("Processing CASH payment: {}", referenceNumber);

        // Cash payment is immediate
        return PaymentResponse.builder()
                .status("COMPLETED")
                .message("Cash payment completed successfully")
                .referenceNumber(referenceNumber)
                .paymentMethod(PaymentMethod.CASH.name())
                .amount(request.getAmount())
                .transactionId("CASH-" + System.currentTimeMillis())
                .build();
    }

    @Override
    protected PaymentResponse processGatewayRefund(Payment payment, BigDecimal amount, String reason) {
        return null;
    }
}