package com.microservice.payment_service.service;


import com.aditya.contracts.event.DomainEvent;
import com.microservice.payment_service.adapter.GatewayAdapter;
import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.entity.*;
import com.microservice.payment_service.messaging.PaymentEventFactory;
import com.microservice.payment_service.outbox.service.OutboxService;
import com.microservice.payment_service.repository.PaymentGatewayResponseRepository;
import com.microservice.payment_service.repository.PaymentRepository;
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

    private final GatewayAdapterService gatewayAdapterService;
    private final PaymentEventFactory paymentEventFactory;
    private final OutboxService outboxService;
    private final PaymentRepository paymentRepository;

    /**
     * Create a payment - handles idempotency & delegates to gateway.
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto request) {

        UUID orderId = request.getReferenceId();
        // for payment => Initiated , then captured and then may be failed
        // for transaction status => created , Authorized , captured , failed
        // 🔥 STEP 1: Find or create Payment (AGGREGATE)
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Payment newPayment = Payment.builder()
                            .orderId(orderId)
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .gateway(request.getGateway())
                            .status(PaymentStatus.INITIATED)
                            .build();
                    return paymentRepository.save(newPayment);
                });
        //Ques   When the new payment is created it paymentId is auto generated UUID if not where I am generating it
        // Ques  tell me each transction is the attempt ,
        //Ques   Somewhere i wrtie Initiated and somewhere create please clerfyt the which status comes when

        // 🔥 STEP 2: Create Transaction (attempt)
        PaymentTransaction tx = PaymentTransaction.builder()
                .paymentId(payment.getPaymentId())   // 🔥 KEY FIX
                .orderId(orderId)
                .userId(request.getUserId().toString())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .gateway(request.getGateway())
                .status(PaymentStatus.CREATED)
                .attemptNumber(getNextAttemptNumber(payment.getPaymentId()))
                .build();

        paymentTransactionRepository.save(tx);

        try {
            // 🔥  STEP 3: Gateway call
            GatewayAdapter adapter = gatewayAdapterService.getAdapter(request.getGateway());
            PaymentGatewayResponse response = adapter.createPayment(request, tx);
            System.out.println("the body is "+response.getBody());
            System.out.println("the mesaage is "+response.getMessage());
            System.out.println("the Id is "+response.getId());

            // 🔥 STEP 4: Save ONLY orderId here
            String gatewayOrderId = adapter.extractGatewayOrderId(response);
            payment.setGatewayOrderId(gatewayOrderId);

            // 🔥 Payment still INITIATED (NOT AUTHORIZED yet)
            payment.setStatus(PaymentStatus.INITIATED);
            paymentRepository.save(payment);

            tx.setStatus(PaymentStatus.CREATED);
            paymentTransactionRepository.save(tx);

            // 🔥 STEP 6: EVENT (IMPORTANT FIX)
            DomainEvent<?> event = paymentEventFactory.createPaymentInitiatedEvent(payment);

            outboxService.saveEvent(
                    payment.getPaymentId(),   // 🔥 aggregateId MUST be paymentId
                    "PAYMENT",
                    "payment.initiated",
                    event
            );

            return PaymentResponseDto.builder()
                    .paymentId(tx.getPaymentId())                  // 🔥 IMPORTANT
                    .transactionId(tx.getTransactionId())          // UUID
                    .gatewayPaymentId(tx.getGatewayPaymentId())    // null until capture
                    .gateway(tx.getGateway())
                    .status(tx.getStatus())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .message(buildMessage(tx))
                    .build();

        } catch (Exception ex) {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            DomainEvent<?> event = paymentEventFactory.createPaymentFailedEvent(payment, ex.getMessage());

            outboxService.saveEvent(
                    payment.getPaymentId(),
                    "PAYMENT",
                    "payment.failed",
                    event
            );

            throw new RuntimeException("Payment failed", ex);
        }
    }

    /**
     * Capture a previously authorized payment.
     */
    public void capturePayment(PaymentTransaction tx) {

        if (tx.getGatewayPaymentId() == null) {
            throw new IllegalStateException("Missing gatewayPaymentId");
        }

        Payment payment = paymentRepository.findByPaymentId(tx.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Idempotency check
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return;
        }

        try {
            GatewayAdapter adapter = gatewayAdapterService.getAdapter(payment.getGateway());

            PaymentGatewayResponse resp = adapter.capturePayment(tx);

            // ✅ UPDATE SAME TRANSACTION
            tx.setStatus(PaymentStatus.CAPTURED);
            tx.setCapturedAt(Instant.now());
            paymentTransactionRepository.save(tx);

            // ✅ UPDATE AGGREGATE
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(Instant.now());
            payment.setCapturedAmount(payment.getAmount());
            paymentRepository.save(payment);

            // ✅ EVENT
            DomainEvent<?> event =
                    paymentEventFactory.createPaymentCompletedEvent(payment, tx);

            outboxService.saveEvent(
                    payment.getPaymentId(),
                    "PAYMENT",
                    "payment.completed",
                    event
            );



        } catch (Exception ex) {

            tx.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(tx);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            DomainEvent<?> event =
                    paymentEventFactory.createPaymentFailedEvent(payment, ex.getMessage());

            outboxService.saveEvent(
                    payment.getPaymentId(),
                    "PAYMENT",
                    "payment.failed",
                    event
            );

            throw new RuntimeException("Capture failed", ex);
        }
    }



    /**
     * Utility to get payment by ID.
     */
    public Optional<PaymentTransaction> getPayment(Long id) {
        return paymentTransactionRepository.findById(id);
    }
    private int getNextAttemptNumber(UUID paymentId) {
         // make the countByPaymentId in the repository
        return paymentTransactionRepository.countByPaymentId(paymentId) + 1;
    }

    private String buildMessage(PaymentTransaction tx) {

        return switch (tx.getStatus()) {
            case CREATED -> "Payment created. Awaiting authorization.";
            case AUTHORIZED -> "Payment authorized. Ready for capture.";
            case CAPTURED -> "Payment successful.";
            case FAILED -> "Payment failed.";
            default -> "Payment processing.";
        };
    }

    private PaymentResponseDto buildResponseFromPayment(Payment payment) {

        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(null) // no active transaction
                .gatewayPaymentId(null)
                .gateway(payment.getGateway())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .message("Payment already completed")
                .build();
    }

    public void retryCapture(Payment payment) {

        log.info("[Retry] Triggering capture retry for payment={}", payment.getPaymentId());

        PaymentTransaction newTx = PaymentTransaction.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId("SYSTEM_RETRY")
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gateway(payment.getGateway())
                .status(PaymentStatus.CREATED)
                .attemptNumber(getNextAttemptNumber(payment.getPaymentId()))
                .build();

        paymentTransactionRepository.save(newTx);

        // 🔥 IMPORTANT: call same capture logic
        capturePayment(newTx);
    }




}

