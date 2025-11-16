package com.microservice.payment_service.dto;


import com.microservice.payment_service.entity.Gateway;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    /**
     * User initiating the payment.
     */
    private String userId;

    /**
     * The total payment amount.
     */
    private BigDecimal amount;

    /**
     * Currency code (e.g. "INR", "USD").
     */
    private String currency;

    /**
     * Gateway to be used (Razorpay, Stripe, PayPal, etc.)
     */
    private Gateway gateway;


    /**
     * Optional business reference (e.g. orderId, invoiceId).
     */
    private String referenceId;


}

