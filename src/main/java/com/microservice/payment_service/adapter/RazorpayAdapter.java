package com.microservice.payment_service.adapter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.adapter.feign.RazorpayFeignClient;
import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.entity.PaymentGatewayResponse;
import com.microservice.payment_service.entity.PaymentTransaction;
import com.microservice.payment_service.entity.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class RazorpayAdapter implements GatewayAdapter {
    private final RazorpayFeignClient razorpayFeignClient;

    @Override
    public PaymentGatewayResponse createPayment(PaymentRequestDto request, PaymentTransaction tx) {
        log.info("[Razorpay] Creating Razorpay Order for user={} amount={}", request.getUserId(), request.getAmount());

        // Razorpay expects amount in paise → multiply by 100
        long razorpayAmount = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Build request body for Razorpay
        Map<String, Object> body = new HashMap<>();
        body.put("amount", razorpayAmount);
        body.put("currency", request.getCurrency());
        body.put("receipt", "receipt_" + tx.getId());
        body.put("payment_capture", 0); // auto-capture disabled , caputre call i make by myself

        try {
            // REAL API CALL
            Map<String, Object> response = razorpayFeignClient.createOrder(body);



            return PaymentGatewayResponse.builder()
                    .gateway(Gateway.RAZORPAY)
                    .statusCode(200)
                    .body(response.toString())
                    .message("Order created successfully")
                    .createdAt(Instant.now())
                    .build();

        } catch (Exception ex) {
            log.error("Error creating Razorpay order: {}", ex.getMessage(), ex);

            return PaymentGatewayResponse.builder()
                    .gateway(Gateway.RAZORPAY)
                    .statusCode(500)
                    .message("Failed to create order at Razorpay")
                    .createdAt(Instant.now())
                    .body(ex.getMessage())
                    .build();
        }
    }


    @Override
    public PaymentGatewayResponse capturePayment(PaymentTransaction tx) {
        log.info("[Razorpay] Capturing payment for externalId={}", tx.getExternalId());

        return PaymentGatewayResponse.builder()
                .gateway(Gateway.RAZORPAY)
                .statusCode(200)
                .body("{\"id\":\"" + tx.getExternalId() + "\",\"status\":\"captured\"}")
                .message("Payment captured successfully")
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public PaymentGatewayResponse initiateRefund(PaymentTransaction tx, Refund refund) {
        log.info("[Razorpay] Initiating refund for transaction={} amount={}", tx.getExternalId(), refund.getAmount());
        String fakeRefundId = "refund_" + UUID.randomUUID();

        return PaymentGatewayResponse.builder()
                .gateway(Gateway.RAZORPAY)
                .statusCode(200)
                .body("{\"refund_id\":\"" + fakeRefundId + "\",\"status\":\"success\"}")
                .message("Refund successful")
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String extractTransactionId(PaymentGatewayResponse response) {
        try {
            JsonNode json = new ObjectMapper().readTree(response.getBody());
            return json.get("id").asText();
        } catch (Exception e) {
            return null;
        }
    }

}

