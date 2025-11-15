package com.microservice.payment_service.dto;


import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    /**
     * Internal transaction ID in your DB.
     */
    private Long transactionId;

    /**
     * External transaction ID (gateway).
     */
    private String externalId;

    /**
     * Payment gateway used.
     */
    private Gateway gateway;

    /**
     * Current payment status.
     */
    private PaymentStatus status;

    /**
     * Payment amount and currency.
     */
    private BigDecimal amount;
    private String currency;

    /**
     * Optional message or note for UI display.
     */
    private String message;
}

