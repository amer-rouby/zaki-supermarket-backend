package com.zakisupermarket.service.Payment;

import com.zakisupermarket.dto.request.PaymentRequest;
import com.zakisupermarket.dto.response.PaymentResponse;
import com.zakisupermarket.entity.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse refundPayment(String paymentReference, BigDecimal amount, String reason);

    PaymentResponse cancelPayment(String paymentReference);

    PaymentResponse verifyPayment(String paymentReference);

    PaymentMethod getPaymentMethod();

    boolean isSupported(PaymentMethod method);
}