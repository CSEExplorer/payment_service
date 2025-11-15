package com.microservice.payment_service.service;


import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;
import com.microservice.payment_service.entity.PaymentTransaction;
import com.microservice.payment_service.repository.PaymentTransactionRepository;
import com.microservice.payment_service.utility.RazorpayWebhookParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private  final PaymentService paymentService;
    private final PaymentTransactionRepository txRepo;
    private final RazorpayWebhookParser parser;

    public void handleGatewayWebhook(String gateway, RazorpayWebhookDto callback) {

        if (!gateway.equalsIgnoreCase("razorpay")) {
            log.warn("Unsupported gateway");
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
                log.warn("Unknown event received: {}", event);
        }
    }

    private void handlePaymentFailed(RazorpayWebhookDto callback) {
    }

    private void handlePaymentCaptured(RazorpayWebhookDto callback) {
    }

    private void  handlePaymentAuthorized(RazorpayWebhookDto dto) {

        String paymentId = parser.getPaymentId(dto);
        String orderId   = parser.getOrderId(dto);
        Long amount      = parser.getAmount(dto);

        log.info("[Webhook] Payment authorized. paymentId={} orderId={} amount={}",
                paymentId, orderId, amount);

        // 1. Lookup PaymentTransaction using orderId
        PaymentTransaction tx = txRepo.findByExternalId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 2. Trigger manual capture
        PaymentResponseDto paymentResponseDto = paymentService.capturePayment(paymentId);
        log.info("[Webhook] Capture API triggered for paymentId={}", paymentId);

    }


}


