package com.microservice.payment_service.adapter;


import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.entity.PaymentGatewayResponse;
import com.microservice.payment_service.entity.PaymentTransaction;
import com.microservice.payment_service.entity.Refund;

/**
 * Common interface to interact with various payment gateways.
 */
public interface GatewayAdapter {

    /**
     * Create a payment with gateway (e.g., generate order, intent, etc.)
     */
    PaymentGatewayResponse createPayment(PaymentRequestDto request, PaymentTransaction tx);

    /**
     * Capture the authorized payment (if applicable).
     */
    PaymentGatewayResponse capturePayment(PaymentTransaction tx);

    /**
     * Initiate a refund for a captured transaction.
     */
    PaymentGatewayResponse initiateRefund(PaymentTransaction tx, Refund refund);

    /**
     * Extracts a gateway-specific transaction id from response.
     */
    String extractTransactionId(PaymentGatewayResponse response);
}

