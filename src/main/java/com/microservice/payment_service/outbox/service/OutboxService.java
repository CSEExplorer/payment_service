package com.microservice.payment_service.outbox.service;


import com.aditya.contracts.event.DomainEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.outbox.model.OutboxEvent;
import com.microservice.payment_service.outbox.model.OutboxStatus;
import com.microservice.payment_service.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;



    public void saveEvent(UUID aggregateId,
                          String aggregateType,
                          String eventType,
                          DomainEvent<?> domainEvent) {

        try {
            String payloadJson = objectMapper.writeValueAsString(domainEvent.getPayload());

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .nextRetryAt(Instant.now())
                    .createdAt(Instant.now())
                    .payload(payloadJson)
                    .build();

            repository.save(event);

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize domain event", e);
        }
    }
}