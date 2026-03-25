package com.microservice.payment_service.service;


import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.order.OrderCreatedEvent;
import com.aditya.contracts.payment.PaymentCompletedEvent;
import com.aditya.contracts.payment.PaymentFailedEvent;
import com.microservice.payment_service.adapter.GatewayAdapter;
import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.entity.*;
import com.microservice.payment_service.messaging.PaymentEventFactory;
import com.microservice.payment_service.outbox.service.OutboxService;
import com.microservice.payment_service.repository.PaymentOperationRepository;
import com.microservice.payment_service.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentOperationRepository paymentOperationRepository;
    private final GatewayAdapterService gatewayAdapterService;
    private final PaymentEventFactory paymentEventFactory;
    private final OutboxService outboxService;
    /**
     * Create a payment - handles idempotency & delegates to gateway.
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        log.info("Initiating payment for user={} amount={}", request.getUserId(), request.getAmount());
        String idempotencyKey = request.getReferenceId() + ":create";
        // --- Step 1: Idempotency check ---
        Optional<PaymentOperation> existingOp = paymentOperationRepository
                .findByIdempotencyKeyAndUserIdAndOperationType(
                        idempotencyKey,
                        request.getUserId(),
                        OperationType.CREATE_PAYMENT
                );

        if (existingOp.isPresent() && existingOp.get().getStatus() == OperationStatus.SUCCESS) {
            log.info("Duplicate payment request detected for idempotencyKey={}, returning existing result", idempotencyKey);
            PaymentTransaction tx = existingOp.get().getPaymentTransaction();
            return PaymentResponseDto.builder()
                    .transactionId(tx.getId())
                    .status(tx.getStatus())
                    .gateway(tx.getGateway())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .build();
        }

        // --- Step 2: Create operation record ---
        PaymentOperation operation = PaymentOperation.builder()
                .idempotencyKey(idempotencyKey)
                .userId(request.getUserId())
                .operationType(OperationType.CREATE_PAYMENT)
                .status(OperationStatus.IN_PROGRESS)
                .requestPayload(request.toString())
                .build();
        paymentOperationRepository.save(operation);

        try {
            // --- Step 3: Create transaction record ---
            PaymentTransaction tx = PaymentTransaction.builder()
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .gateway(request.getGateway())
                    .status(PaymentStatus.CREATED)
                    .referenceId(UUID.fromString(request.getReferenceId()))
                    .build();
            paymentTransactionRepository.save(tx);
            operation.setPaymentTransaction(tx);

            // first state created comes and then Authorized comes
            // --- Step 4: Delegate to gateway ---
            GatewayAdapter adapter = gatewayAdapterService.getAdapter(request.getGateway());
            PaymentGatewayResponse gatewayResponse = adapter.createPayment(request, tx);
            System.out.println("the body is "+gatewayResponse.getBody());
            System.out.println("the mesaage is "+gatewayResponse.getMessage());
            System.out.println("the Id is "+gatewayResponse.getId());

            // --- Step 5: Persist response ---
            operation.setGatewayResponse(gatewayResponse);
            operation.setStatus(OperationStatus.SUCCESS);
            paymentOperationRepository.save(operation);

            tx.setExternalId(adapter.extractTransactionId(gatewayResponse));
            tx.setStatus(PaymentStatus.AUTHORIZED);
            paymentTransactionRepository.save(tx);


            // --- Step 6: Build response ---
            return PaymentResponseDto.builder()
                    .transactionId(tx.getId())
                    .status(tx.getStatus())
                    .gateway(tx.getGateway())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .externalId(tx.getExternalId())
                    .build();


        } catch (Exception ex) {
            //  Here i will publish payment Successfully
            log.error("Payment creation failed: {}", ex.getMessage(), ex);
            PaymentFailedEvent payload = PaymentFailedEvent.builder()
                    .paymentId(null)
                    .orderId(null)
                    .userId(UUID.fromString(request.getUserId()))
                    .amount(request.getAmount())
                    .reason(ex.getMessage()).build();

            DomainEvent<?> event =
                    paymentEventFactory.createPaymentFailedEvent(UUID.fromString(request.getReferenceId()),payload);


            outboxService.saveEvent(
                    UUID.fromString(request.getReferenceId()),
                    "PAYMENT",
                    "payment.failed",
                    event
            );

            operation.setStatus(OperationStatus.FAILED);
            paymentOperationRepository.save(operation);
            throw new RuntimeException("Payment creation failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Capture a previously authorized payment.
     */
    @Transactional
    public PaymentResponseDto capturePayment(String externalOrderId) {

        // 1. Find the PaymentTransaction
        PaymentTransaction tx = paymentTransactionRepository.findByExternalId(externalOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + externalOrderId));

        // 2. If already captured → return existing response
        if (tx.getStatus() == PaymentStatus.CAPTURED) {
            return PaymentResponseDto.builder()
                    .transactionId(tx.getId())
                    .status(tx.getStatus())
                    .gateway(tx.getGateway())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .externalId(tx.getExternalId())
                    .build();
        }

        /*
         * IDEMPOTENCY KEY:
         * For capture operations, safest is:
         *     <order_id> + ":capture"
         */
        String idempotencyKey = tx.getExternalId() + ":capture";

        // 3. Look for existing PaymentOperation (idempotency check)
        Optional<PaymentOperation> existingOp =
                paymentOperationRepository.findByIdempotencyKeyAndUserIdAndOperationType(
                        idempotencyKey,
                        tx.getUserId(),
                        OperationType.CAPTURE_PAYMENT
                );

        if (existingOp.isPresent() && existingOp.get().getStatus() == OperationStatus.SUCCESS) {
            // Capture already done earlier → return without hitting gateway
            return PaymentResponseDto.builder()
                    .transactionId(tx.getId())
                    .status(PaymentStatus.CAPTURED)
                    .gateway(tx.getGateway())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .externalId(tx.getExternalId())
                    .build();
        }

        // 4. Create a new PaymentOperation entry
        PaymentOperation op = PaymentOperation.builder()
                .idempotencyKey(idempotencyKey)
                .operationType(OperationType.CAPTURE_PAYMENT)
                .status(OperationStatus.IN_PROGRESS)
                .userId(tx.getUserId())
                .paymentTransaction(tx)
                .requestPayload("Capture for payment orderId=" + tx.getExternalId())
                .createdAt(Instant.now())
                .build();

        paymentOperationRepository.save(op);

        try {
            // 5. Call Gateway
            GatewayAdapter adapter = gatewayAdapterService.getAdapter(tx.getGateway());
            PaymentGatewayResponse resp = adapter.capturePayment(tx);

            // 6. Mark operation SUCCESS
            op.setStatus(OperationStatus.SUCCESS);
            op.setRequestPayload(resp.getBody());
            paymentOperationRepository.save(op);

            // 7. Update main transaction
            tx.setStatus(PaymentStatus.CAPTURED);
            tx.setCapturedAt(Instant.now());
            paymentTransactionRepository.save(tx);

            PaymentCompletedEvent payload = PaymentCompletedEvent.builder()
                    .paymentId(null)
                    .orderId(tx.getReferenceId())
                    .userId(UUID.fromString(tx.getUserId()))
                    .amount(tx.getAmount())
                    .build();
            DomainEvent<?> event =
                    paymentEventFactory.createPaymentCompletedEvent(tx.getReferenceId(),payload);


            outboxService.saveEvent(
                    tx.getReferenceId(),
                    "PAYMENT",
                    "payment.completed",
                    event
            );
            System.out.println("Payment Captured Published to the rabbitMQ");

            // 8. Build and return response
            return PaymentResponseDto.builder()
                    .transactionId(tx.getId())
                    .status(PaymentStatus.CAPTURED)
                    .gateway(tx.getGateway())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .externalId(tx.getExternalId())
                    .build();

        } catch (Exception ex) {
            PaymentFailedEvent payload = PaymentFailedEvent.builder()
                    .paymentId(null)
                    .orderId(null)
                    .userId(UUID.fromString(tx.getUserId()))
                    .amount(tx.getAmount())
                    .reason(ex.getMessage()).build();
            DomainEvent<?> event =
                    paymentEventFactory.createPaymentFailedEvent(tx.getReferenceId(),payload);


            outboxService.saveEvent(
                    tx.getReferenceId(),
                    "PAYMENT",
                    "payment.failed",
                    event
            );
            // 9. Mark operation FAILED
            op.setStatus(OperationStatus.FAILED);
            op.setRequestPayload("ERROR: " + ex.getMessage());
            paymentOperationRepository.save(op);



            throw new RuntimeException("Failed to capture payment: " + ex.getMessage(), ex);
        }
    }


    /**
     * Utility to get payment by ID.
     */
    public Optional<PaymentTransaction> getPayment(Long id) {
        return paymentTransactionRepository.findById(id);
    }
}

