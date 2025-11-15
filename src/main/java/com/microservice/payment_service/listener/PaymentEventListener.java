package com.microservice.payment_service.listener;


import com.microservice.payment_service.event.PaymentCapturedEvent;
import com.microservice.payment_service.event.PaymentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventListener {

    @KafkaListener(topics = "payment_created", groupId = "payment-service")
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        log.info("[Kafka] Payment Created Event received: transactionId={}", event.getTransactionId());
        // TODO: send email, send SMS, notify order service, etc.
    }

    @KafkaListener(topics = "payment_captured", groupId = "payment-service")
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        log.info("[Kafka] Payment Captured Event received: transactionId={}", event.getTransactionId());
        // TODO: update analytics, trigger fulfillment, etc.
    }
}

