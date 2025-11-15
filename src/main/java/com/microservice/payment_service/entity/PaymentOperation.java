package com.microservice.payment_service.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_operations",
        uniqueConstraints = {
                // ensure idempotency across user and operation type + idempotency key
                @UniqueConstraint(name = "uk_operation_idempotency",
                        columnNames = {"idempotency_key", "user_id", "operation_type"})
        },
        indexes = {
                @Index(name = "idx_operation_operation_id", columnList = "operation_id"),
                @Index(name = "idx_operation_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A globally unique id for this operation (UUID).
     */
    @Column(name = "operation_id", nullable = false, unique = true, updatable = false)
    private String operationId;

    /**
     * Idempotency key provided by client to dedupe requests (optional).
     */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OperationStatus status;

    /**
     * Optional JSON payload or request data that triggered this operation.
     */

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * Optional gateway response object (one-to-one).
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "gateway_response_id")
    private PaymentGatewayResponse gatewayResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        if (operationId == null) operationId = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = OperationStatus.PENDING;
    }
}
