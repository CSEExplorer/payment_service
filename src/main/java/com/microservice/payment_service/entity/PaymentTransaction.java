package com.microservice.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_external_id", columnList = "external_id"),
                @Index(name = "idx_payment_status", columnList = "status")
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
     * ID returned by gateway (if any). Nullable until gateway responds.
     */
    @Column(name = "external_id", unique = true)
    private String externalId;

    /**
     * Reference to the user or merchant who initiated the payment.
     * Keep as String for flexibility (UUID, numeric id, etc.)
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Business reference or order id in your system.
     */
    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway")
    private Gateway gateway;

    /**
     * Amount already refunded (aggregate).
     */
    @Column(name = "refunded_amount", precision = 19, scale = 4)
    private BigDecimal refundedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @OneToMany(mappedBy = "paymentTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentOperation> operations = new ArrayList<>();

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
    public void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = createdAt;
        if (refundedAmount == null) refundedAmount = BigDecimal.ZERO;
        if (status == null) status = PaymentStatus.CREATED;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    // helper
    public void addOperation(PaymentOperation op) {
        operations.add(op);
        op.setPaymentTransaction(this);
    }

    public void addRefund(Refund r) {
        refunds.add(r);
        r.setPaymentTransaction(this);
    }
}

