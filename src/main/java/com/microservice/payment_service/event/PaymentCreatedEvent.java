package com.microservice.payment_service.event;

import com.microservice.payment_service.entity.Gateway;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreatedEvent {
    private Long transactionId;
    private String externalId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private Gateway gateway;
    private Instant createdAt;
}
