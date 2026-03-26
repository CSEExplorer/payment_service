package com.microservice.payment_service.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.adapter.feign.RazorpayFeignClient;
import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.entity.PaymentGatewayResponse;
import com.microservice.payment_service.entity.PaymentTransaction;
import com.microservice.payment_service.entity.Refund;
import com.microservice.payment_service.repository.PaymentGatewayResponseRepository;
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
    private final PaymentGatewayResponseRepository gatewayResponseRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentGatewayResponse createPayment(PaymentRequestDto request, PaymentTransaction tx) {

        log.info("[Razorpay] Creating Order user={} amount={}", request.getUserId(), request.getAmount());

        long razorpayAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", razorpayAmount);
        body.put("currency", request.getCurrency());
        body.put("receipt", generateReceipt(tx));
        body.put("payment_capture", 0);

        try {
            Map<String, Object> response = razorpayFeignClient.createOrder(body);
            String json = objectMapper.writeValueAsString(response);

            // ✅ Save success response
            PaymentGatewayResponse saved = saveGatewayResponse(
                    Gateway.RAZORPAY,
                    200,
                    json,
                    "Order created successfully"
            );

            // ✅ Extract important fields
            JsonNode node = objectMapper.readTree(json);
            String razorpayOrderId = node.get("id").asText();



            log.info("[Razorpay] Order created: orderId={} receipt={}",
                    razorpayOrderId, node.get("receipt").asText());

            return saved;

        } catch (Exception ex) {

            log.error("[Razorpay] Order creation FAILED for tx={}", tx.getTransactionId(), ex);

            String errorBody = extractErrorBody(ex);

            return saveGatewayResponse(
                    Gateway.RAZORPAY,
                    500,
                    errorBody,
                    "Order creation failed"
            );
        }
    }

    @Override
    public PaymentGatewayResponse capturePayment(PaymentTransaction tx) {

        log.info("[Razorpay] Capturing payment paymentId={} amount={}",
                tx.getGatewayPaymentId(), tx.getAmount());

        try {
            if (tx.getGatewayPaymentId() == null) {
                throw new IllegalArgumentException("Missing Razorpay paymentId");
            }

            long amountInPaise = tx.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Map<String, Object> body = new HashMap<>();
            body.put("amount", amountInPaise);
            body.put("currency", tx.getCurrency());

            Map<String, Object> response =
                    razorpayFeignClient.capturePayment(tx.getGatewayPaymentId(), body);

            String json = objectMapper.writeValueAsString(response);

            // ✅ Save success
            PaymentGatewayResponse saved = saveGatewayResponse(
                    Gateway.RAZORPAY,
                    200,
                    json,
                    "Payment captured"
            );

            // ✅ Extract payment info
            JsonNode node = objectMapper.readTree(json);


            log.info("[Razorpay] Payment captured: paymentId={} status={}",
                    node.get("id").asText(),
                    node.get("status").asText());

            return saved;

        } catch (Exception ex) {

            log.error("[Razorpay] Capture FAILED paymentId={}", tx.getGatewayPaymentId(), ex);

            return saveGatewayResponse(
                    Gateway.RAZORPAY,
                    500,
                    extractErrorBody(ex),
                    "Capture failed"
            );
        }
    }

    @Override
    public PaymentGatewayResponse initiateRefund(PaymentTransaction tx, Refund refund) {

        log.info("[Razorpay] Refund initiated paymentId={} amount={}",
                tx.getGatewayPaymentId(), refund.getAmount());

        try {
            String fakeRefundId = "refund_" + UUID.randomUUID();

            String json = String.format(
                    "{\"refund_id\":\"%s\",\"status\":\"processed\"}",
                    fakeRefundId
            );

            return saveGatewayResponse(
                    Gateway.RAZORPAY,
                    200,
                    json,
                    "Refund processed"
            );

        } catch (Exception ex) {

            log.error("[Razorpay] Refund FAILED", ex);

            return saveGatewayResponse(
                    Gateway.RAZORPAY,
                    500,
                    extractErrorBody(ex),
                    "Refund failed"
            );
        }
    }

    @Override
    public String extractGatewayOrderId(PaymentGatewayResponse response) {
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.has("id") ? json.get("id").asText() : null;
        } catch (Exception e) {
            log.error("Failed to extract orderId", e);
            return null;
        }
    }

    // ================== HELPERS ==================

    private PaymentGatewayResponse saveGatewayResponse(Gateway gateway,
                                                       int statusCode,
                                                       String body,
                                                       String message) {

        PaymentGatewayResponse response = PaymentGatewayResponse.builder()
                .gateway(gateway)
                .statusCode(statusCode)
                .body(body)
                .message(message)
                .createdAt(Instant.now())
                .build();

        return gatewayResponseRepository.save(response);
    }

    private String extractErrorBody(Exception ex) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("error", ex.getMessage())
            );
        } catch (Exception e) {
            return "{\"error\":\"unknown\"}";
        }
    }

    private String generateReceipt(PaymentTransaction tx) {
        return "ord_" + tx.getTransactionId()
                .toString()
                .replace("-", "")
                .substring(0, 20); // <= 40 chars safe
    }
}