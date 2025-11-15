package com.microservice.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/{gateway}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable String gateway,
            @RequestBody RazorpayWebhookDto callback,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {

        log.info("[Webhook] Received callback from gateway={} eventType={}",
                gateway, callback.getEvent());

        try {
            // 1. Convert to JSON string (store raw payload)
            String payloadJson = objectMapper.writeValueAsString(callback);

            // 2. Push to Kafka topic for async processing
            kafkaTemplate.send("payment_webhook_events", payloadJson);

            log.info("[Webhook] Published event to Kafka topic=payment_webhook_events");

        } catch (Exception e) {
            log.error("[Webhook] ERROR pushing event to Kafka", e);
            return ResponseEntity.status(500).body("Failed");
        }

        // 3. Always return 200 immediately
        // Razorpay retries aggressively if response isn't 200.
        return ResponseEntity.ok("Webhook received");
    }
}


