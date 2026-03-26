package com.microservice.payment_service.service;


import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;
import com.microservice.payment_service.entity.Payment;
import com.microservice.payment_service.entity.PaymentStatus;
import com.microservice.payment_service.entity.PaymentTransaction;
import com.microservice.payment_service.repository.PaymentRepository;
import com.microservice.payment_service.repository.PaymentTransactionRepository;
import com.microservice.payment_service.utility.RazorpayWebhookParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final PaymentService paymentService;
    private final PaymentTransactionRepository txRepo;
    private final PaymentRepository paymentRepository;
    private final RazorpayWebhookParser parser;

    public void handleGatewayWebhook(String gateway, RazorpayWebhookDto callback) {

        if (!"razorpay".equalsIgnoreCase(gateway)) {
            log.warn("Unsupported gateway: {}", gateway);
            return;
        }

        String event = callback.getEvent();

        switch (event) {

            case "payment.authorized":
                handlePaymentAuthorized(callback);
                break;

            case "payment.captured":
                handlePaymentCaptured(callback);
                break;

            case "payment.failed":
                handlePaymentFailed(callback);
                break;

            default:
                log.warn("Unknown webhook event: {}", event);
        }
    }

    /**
     * PAYMENT AUTHORIZED → store paymentId → trigger capture
     */
    private void handlePaymentAuthorized(RazorpayWebhookDto dto) {

        String gatewayPaymentId = parser.getPaymentId(dto);
        String gatewayOrderId = parser.getOrderId(dto);

        log.info("[Webhook] AUTHORIZED paymentId={} orderId={}",
                gatewayPaymentId, gatewayOrderId);

        // 🔥 STEP 1: Find Payment via gatewayOrderId
        Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 🔥 STEP 2: Find latest transaction
        PaymentTransaction tx = txRepo
                .findTopByPaymentIdOrderByAttemptNumberDesc(payment.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 🔥 Idempotency check
        if (tx.getStatus() == PaymentStatus.AUTHORIZED ||
                tx.getStatus() == PaymentStatus.CAPTURED) {
            log.info("Already processed AUTHORIZED webhook");
            return;
        }

        // 🔥 STEP 3: Update transaction in the payment service
        tx.setGatewayPaymentId(gatewayPaymentId);
        tx.setStatus(PaymentStatus.AUTHORIZED);
        txRepo.save(tx);

        // 🔥 STEP 4: Trigger capture
        paymentService.capturePayment(tx);

        log.info("[Webhook] Capture triggered for paymentId={}", gatewayPaymentId);
    }

    /**
     * PAYMENT CAPTURED → final success
     */
    private void handlePaymentCaptured(RazorpayWebhookDto dto) {
        //Extra Written code

        String gatewayPaymentId = parser.getPaymentId(dto);

        log.info("[Webhook] CAPTURED paymentId={}", gatewayPaymentId);

        PaymentTransaction tx = txRepo.findByGatewayPaymentId(gatewayPaymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (tx.getStatus() == PaymentStatus.CAPTURED) {
            log.info("Already captured");
            return;
        }

        tx.setStatus(PaymentStatus.CAPTURED);
        txRepo.save(tx);

        Payment payment = paymentRepository.findByPaymentId(tx.getPaymentId())
                .orElseThrow();

        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);
    }

    /**
     * PAYMENT FAILED
     */
    private void handlePaymentFailed(RazorpayWebhookDto dto) {

        String gatewayPaymentId = parser.getPaymentId(dto);

        log.info("[Webhook] FAILED paymentId={}", gatewayPaymentId);

        PaymentTransaction tx = txRepo.findByGatewayPaymentId(gatewayPaymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (tx.getStatus() == PaymentStatus.FAILED) {
            log.info("Already failed");
            return;
        }

        tx.setStatus(PaymentStatus.FAILED);
        txRepo.save(tx);

        Payment payment = paymentRepository.findByPaymentId(tx.getPaymentId())
                .orElseThrow();

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }
}