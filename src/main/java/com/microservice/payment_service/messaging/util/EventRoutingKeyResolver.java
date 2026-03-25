package com.microservice.payment_service.messaging.util;


import com.aditya.contracts.event.EventTypes;
import org.springframework.stereotype.Component;

@Component
public class EventRoutingKeyResolver {

    public String resolve(String eventType) {

        return switch (eventType) {
            case EventTypes.PAYMENT_COMPLETED -> "payment.completed";
            case EventTypes.PAYMENT_FAILED -> "payment.failed";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}