package com.microservice.payment_service.repository;

import com.microservice.payment_service.entity.PaymentStatus;
import com.microservice.payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByExternalId(String externalId);

    Optional<PaymentTransaction> findByReferenceId(String referenceId);

    List<PaymentTransaction> findByUserId(String userId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.status IN :statuses")
    List<PaymentTransaction> findAllByStatuses(List<PaymentStatus> statuses);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.createdAt < CURRENT_TIMESTAMP - INTERVAL '7' DAY")
    List<PaymentTransaction> findOldTransactionsForReconciliation();
}

