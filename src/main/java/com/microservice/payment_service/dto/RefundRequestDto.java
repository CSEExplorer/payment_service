package com.microservice.payment_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequestDto {

    /**
     * Transaction ID for which refund is being requested.
     */
    private Long transactionId;

    /**
     * Refund amount (can be full or partial).
     */
    private BigDecimal amount;

    /**
     * Optional reason for refund.
     */
    private String reason;
}

