package com.microservice.payment_service.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-friendly plan name (e.g., "Pro Monthly").
     */
    @Column(nullable = false)
    private String name;

    /**
     * Price for the plan (store in smallest currency unit or use BigDecimal).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /**
     * Currency code (ISO 4217), e.g., "INR", "USD"
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Billing interval (MONTHLY, YEARLY) — simple free text or enum if you prefer.
     */
    @Column(name = "interval")
    private String interval;

    /**
     * Free-text metadata or JSON (small) about the plan.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}

