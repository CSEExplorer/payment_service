package com.microservice.payment_service.messaging.consumer;


import com.aditya.contracts.event.DomainEvent;
import com.aditya.contracts.order.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    @RabbitListener(queues = "payment.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {


        PaymentRequestDto request = PaymentRequestDto.builder()
                .userId(event.getUserId())
                  .amount(event.getTotalAmount())
                .currency("INR")
                .gateway(Gateway.RAZORPAY)
                .referenceId(event.getOrderId()).build();

        paymentService.createPayment(request);
    }
}