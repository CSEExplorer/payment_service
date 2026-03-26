package com.microservice.payment_service.dto;

import com.microservice.payment_service.entity.Gateway;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    private UUID userId;         // 🔥 FIXED
    private BigDecimal amount;
    private String currency;
    private Gateway gateway;

    private UUID referenceId;    // 🔥 orderId (clean now)
}