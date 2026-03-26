package com.microservice.payment_service.messaging;


import com.aditya.contracts.event.AggregateTypes;
import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.event.EventTypes;
import com.aditya.contracts.event.EventVersions;
import com.aditya.contracts.payment.PaymentCompletedEvent;
import com.aditya.contracts.payment.PaymentFailedEvent;
import com.aditya.contracts.payment.PaymentInitiatedEvent;
import com.microservice.payment_service.entity.Payment;
import com.microservice.payment_service.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventFactory {

    /**
     * PAYMENT INITIATED
     */
    public DomainEvent<PaymentInitiatedEvent> createPaymentInitiatedEvent(Payment payment) {

        PaymentInitiatedEvent payload = PaymentInitiatedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(null) // 🔥 set if available in Payment
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gatewayOrderId(payment.getGatewayOrderId())
                .build();

        return DomainEvent.of(
                EventTypes.PAYMENT_INITIATED,
                EventVersions.V1,
                AggregateTypes.PAYMENT,
                payment.getPaymentId(),
                payload
        );
    }

    /**
     * PAYMENT COMPLETED
     */
    public DomainEvent<PaymentCompletedEvent> createPaymentCompletedEvent(
            Payment payment,
            PaymentTransaction transaction
    ) {

        PaymentCompletedEvent payload = PaymentCompletedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(null) // 🔥 set if available
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gatewayPaymentId(transaction.getGatewayPaymentId())
                .build();

        return DomainEvent.of(
                EventTypes.PAYMENT_COMPLETED,
                EventVersions.V1,
                AggregateTypes.PAYMENT,
                payment.getPaymentId(),
                payload
        );
    }

    /**
     * PAYMENT FAILED
     */
    public DomainEvent<PaymentFailedEvent> createPaymentFailedEvent(
            Payment payment,
            String reason
    ) {

        PaymentFailedEvent payload = PaymentFailedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(null) // 🔥 set if available
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .reason(reason)
                .build();

        return DomainEvent.of(
                EventTypes.PAYMENT_FAILED,
                EventVersions.V1,
                AggregateTypes.PAYMENT,
                payment.getPaymentId(),
                payload
        );
    }
}