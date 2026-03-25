package com.microservice.payment_service.messaging;


import com.aditya.contracts.event.AggregateTypes;
import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.event.EventTypes;
import com.aditya.contracts.event.EventVersions;

import com.aditya.contracts.payment.PaymentCompletedEvent;
import com.aditya.contracts.payment.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentEventFactory {

    public DomainEvent<PaymentCompletedEvent> createPaymentCompletedEvent(UUID aggregrateId , PaymentCompletedEvent payload) {


        return DomainEvent.of(
                EventTypes.PAYMENT_COMPLETED,
                EventVersions.V1,
                AggregateTypes.PAYMENT,
                aggregrateId,
                payload
        );
    }
    public DomainEvent<PaymentFailedEvent> createPaymentFailedEvent(UUID aggregrateId , PaymentFailedEvent payload) {


        return DomainEvent.of(
                EventTypes.PAYMENT_FAILED,
                EventVersions.V1,
                AggregateTypes.PAYMENT,
                aggregrateId,
                payload
        );
    }
}