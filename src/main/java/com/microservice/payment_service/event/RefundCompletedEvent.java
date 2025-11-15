package com.microservice.payment_service.event;

import com.microservice.payment_service.entity.Gateway;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundCompletedEvent {
    private String refundId;
    private Long transactionId;
    private BigDecimal amount;
    private String currency;
    private Gateway gateway;
    private Instant processedAt;
}

