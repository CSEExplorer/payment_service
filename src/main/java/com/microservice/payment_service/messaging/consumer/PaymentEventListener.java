package com.microservice.payment_service.messaging.consumer;


import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.order.OrderCreatedEvent;
import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = "payment.queue")
    public void handleOrderCreated(DomainEvent<OrderCreatedEvent> event) {

        OrderCreatedEvent payload = event.getPayload();
        PaymentRequestDto request = PaymentRequestDto.builder()
                .userId(payload.getUserId())
                .amount(payload.getTotalAmount())
                .currency("INR")
                .gateway(Gateway.RAZORPAY)
                .referenceId(payload.getOrderId()).build();

        paymentService.createPayment(request);
    }
}