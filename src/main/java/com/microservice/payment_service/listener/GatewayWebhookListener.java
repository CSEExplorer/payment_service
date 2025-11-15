package com.microservice.payment_service.listener;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;
import com.microservice.payment_service.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayWebhookListener {

    private final ObjectMapper objectMapper;
    private final WebhookService webhookService;

    @KafkaListener(topics = "payment_webhook_events", groupId = "payment-service")
    public void handleWebhookCallback(String message) {

        log.info("[Kafka] Received webhook event message");

        try {
            // 1. Deserialize JSON → WebhookEventDto
            RazorpayWebhookDto callback =
                    objectMapper.readValue(message, RazorpayWebhookDto.class);

            log.info("[Kafka] Event={} received from Razorpay", callback.getEvent());

            // 2. Delegate to service (THIS handles everything)
            webhookService.handleGatewayWebhook("razorpay", callback);

            log.info("[Kafka] Webhook processed successfully event={}", callback.getEvent());

        } catch (Exception e) {

            log.error("[Kafka] Failed to process webhook event", e);

            // 3. TODO → Send to DLQ for retry if needed
            // kafkaTemplate.send("payment_webhook_events_dlq", message);
        }
    }
}


