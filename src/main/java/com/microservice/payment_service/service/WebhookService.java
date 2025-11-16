package com.microservice.payment_service.service;


import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;
import com.microservice.payment_service.entity.PaymentStatus;
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
        System.out.println("reached handlePaymentAuthorized");
        System.out.println("This is the paymentId" + paymentId);

        String orderId   = parser.getOrderId(dto);
        System.out.println("this is orderId"+orderId);
        Long amount      = parser.getAmount(dto);

        log.info("[Webhook] Payment authorized. paymentId={} orderId={} amount={}",
                paymentId, orderId, amount);

        // 1. Lookup PaymentTransaction using orderId
        PaymentTransaction tx = txRepo.findByExternalId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        tx.setPaymentId(paymentId);
        tx.setStatus(PaymentStatus.AUTHORIZED);
        txRepo.save(tx);
        // 2. Trigger manual capture
        PaymentResponseDto paymentResponseDto = paymentService.capturePayment(orderId);
        log.info("[Webhook] Capture API triggered for paymentId={}", paymentId);

    }


}


