package com.microservice.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions",
        indexes = {
                @Index(name = "idx_txn_payment_id", columnList = "payment_id"),
                @Index(name = "idx_txn_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal transaction ID (UUID)
     */
    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId;

    /**
     * Payment aggregate ID (same across retries)
     */
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    /**
     * Order ID from Order Service
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /**
     * Gateway payment ID (razorpay_payment_id)
     */
    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway")
    private Gateway gateway;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Retry attempt number
     */
    @Column(name = "attempt_number")
    private Integer attemptNumber;

    /**
     * Failure reason if any
     */
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "refunded_amount", precision = 19, scale = 4)
    private BigDecimal refundedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;



    @OneToMany(mappedBy = "paymentTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @PrePersist
    public void prePersist() {
        if (transactionId == null) {
            transactionId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;

        if (refundedAmount == null) {
            refundedAmount = BigDecimal.ZERO;
        }

        if (status == null) {
            status = PaymentStatus.CREATED;
        }

        if (attemptNumber == null) {
            attemptNumber = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }



    public void addRefund(Refund refund) {
        refunds.add(refund);
        refund.setPaymentTransaction(this);
    }
}