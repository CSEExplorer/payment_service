package com.microservice.payment_service.repository;


import com.microservice.payment_service.entity.OperationStatus;
import com.microservice.payment_service.entity.OperationType;
import com.microservice.payment_service.entity.PaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, Long> {

    Optional<PaymentOperation> findByOperationId(String operationId);

    Optional<PaymentOperation> findByIdempotencyKeyAndUserIdAndOperationType(
            String idempotencyKey, String userId, OperationType operationType);

    List<PaymentOperation> findByStatus(OperationStatus status);

    @Query("SELECT o FROM PaymentOperation o WHERE o.status = 'RETRY_SCHEDULED'")
    List<PaymentOperation> findAllPendingRetries();
}

