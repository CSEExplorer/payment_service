package com.microservice.payment_service.entity;



import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id")
        },
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

        /**
     * Aggregate ID (used in DomainEvent.aggregateId)
     */
    @Column(name = "payment_id", nullable = false, unique = true, updatable = false)
    private UUID paymentId;

    /**
     * Order ID from Order Service (1:1 mapping)
     */
    @Column(name = "order_id", nullable = false, unique = true, updatable = false)
    private UUID orderId;

    /**
     * Overall payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Total amount to be paid
     */
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    /**
     * Currency (INR, USD, etc.)
     */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Gateway being used (RAZORPAY, STRIPE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway")
    private Gateway gateway;

    /**
     * Gateway order ID (e.g., razorpay_order_id)
     */
    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    /**
     * Total amount captured so far
     */
    @Column(name = "captured_amount", precision = 19, scale = 4)
    private BigDecimal capturedAmount;

    /**
     * Total refunded amount
     */
    @Column(name = "refunded_amount", precision = 19, scale = 4)
    private BigDecimal refundedAmount;

    /**
     * Last failure reason (if any)
     */
    @Column(name = "failure_reason")
    private String failureReason;

    /**
     * Audit fields
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * When payment was successfully captured
     */
    @Column(name = "captured_at")
    private Instant capturedAt;

    // =========================
    // Lifecycle Hooks
    // =========================

    @PrePersist
    public void prePersist() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;

        if (status == null) {
            status = PaymentStatus.CREATED;
        }

        if (capturedAmount == null) {
            capturedAmount = BigDecimal.ZERO;
        }

        if (refundedAmount == null) {
            refundedAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}