package com.microservice.payment_service.repository;

import com.microservice.payment_service.entity.PaymentStatus;
import com.microservice.payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * 🔥 Find latest transaction for a payment (used in webhook)
     */
    Optional<PaymentTransaction> findTopByPaymentIdOrderByAttemptNumberDesc(UUID paymentId);

    /**
     * 🔥 Find transaction using gateway payment id (razorpay_payment_id)
     */
    Optional<PaymentTransaction> findByGatewayPaymentId(String gatewayPaymentId);

    /**
     * 🔥 Find all transactions for a payment
     */
    List<PaymentTransaction> findByPaymentId(UUID paymentId);

    /**
     * 🔥 Find transactions by user
     */
    List<PaymentTransaction> findByUserId(String userId);

    /**
     * 🔥 Find by status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);

    /**
     * 🔥 Find multiple statuses
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status IN :statuses")
    List<PaymentTransaction> findAllByStatuses(List<PaymentStatus> statuses);

    /**
     * 🔥 Used for retry attempt count
     */
    int countByPaymentId(UUID paymentId);
}