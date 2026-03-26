package com.microservice.payment_service.messaging.producer;



import com.aditya.contracts.payment.PaymentCompletedEvent;
import com.aditya.contracts.payment.PaymentFailedEvent;
import com.aditya.contracts.payment.PaymentInitiatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.config.RabbitMQConfig;

import com.microservice.payment_service.outbox.model.OutboxEvent;
import com.microservice.payment_service.outbox.model.OutboxStatus;
import com.microservice.payment_service.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper; // ✅ REQUIRED

    @Scheduled(fixedDelay = 10000)
    public void publishEvents() {

        List<OutboxEvent> events = repository.findProcessableEvents();

        for (OutboxEvent event : events) {
            try {

                Object payload = mapPayload(event); // 🔥 KEY FIX

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        event.getEventType(),
                        payload   // ✅ OBJECT, not String
                );

                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(Instant.now());

            } catch (Exception e) {

                log.error("Failed to publish event {}", event.getId(), e);

                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);

                if (retries >= 5) {
                    log.error("Event {} marked permanently FAILED", event.getId());
                    event.setNextRetryAt(null);
                } else {
                    event.setNextRetryAt(calculateBackoff(retries));
                }

                event.setStatus(OutboxStatus.FAILED);
            }

            repository.save(event);
        }
    }

    // 🔥 CENTRAL FIX METHOD
    private Object mapPayload(OutboxEvent event) throws Exception {

        return switch (event.getEventType()) {

            case "payment.initiated" ->
                    objectMapper.readValue(event.getPayload(), PaymentInitiatedEvent.class);

            case "payment.completed" ->
                    objectMapper.readValue(event.getPayload(), PaymentCompletedEvent.class);

            case "payment.failed" ->
                    objectMapper.readValue(event.getPayload(), PaymentFailedEvent.class);

            default -> throw new RuntimeException("Unknown event type: " + event.getEventType());
        };
    }

    private Instant calculateBackoff(int retryCount) {

        long delaySeconds = switch (retryCount) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 30;
            case 4 -> 60;
            default -> 120;
        };

        return Instant.now().plusSeconds(delaySeconds);
    }
}