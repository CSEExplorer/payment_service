package com.microservice.payment_service.dto;

import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private UUID paymentId;      // 🔥 IMPORTANT
    private UUID transactionId;  // 🔥 better than Long

    private String gatewayPaymentId;

    private Gateway gateway;
    private PaymentStatus status;

    private BigDecimal amount;
    private String currency;

    private String message;
}