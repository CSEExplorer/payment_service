package com.microservice.payment_service.repository;



import com.microservice.payment_service.entity.Payment;
import com.microservice.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 🔥 Find payment using Order ID (used in service layer)
     */
    Optional<Payment> findByOrderId(UUID orderId);

    /**
     * 🔥 Find payment using Payment ID (aggregateId)
     */
    Optional<Payment> findByPaymentId(UUID paymentId);

    /**
     * 🔥 REQUIRED for webhook (VERY IMPORTANT)
     */
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    /**
     * 🔥 Useful for retry / reconciliation jobs
     */
    List<Payment> findByStatus(PaymentStatus status);
}