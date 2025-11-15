package com.microservice.payment_service.repository;

import com.microservice.payment_service.entity.Refund;
import com.microservice.payment_service.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    List<Refund> findByPaymentTransactionId(Long paymentTransactionId);

    List<Refund> findByStatus(RefundStatus status);
}

