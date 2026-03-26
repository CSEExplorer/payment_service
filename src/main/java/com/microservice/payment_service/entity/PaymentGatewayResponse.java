package com.microservice.payment_service.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "payment_gateway_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false)
    private Gateway gateway;

    /**
     * HTTP status code or gateway-specific status code.
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * Raw JSON response from gateway.
     */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * Short readable message (optional).
     */
    @Column(name = "message")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}