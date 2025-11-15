package com.microservice.payment_service.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.dto.callback.RazorpayWebhookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RazorpayWebhookParser {

    private final ObjectMapper objectMapper;

    private JsonNode getRoot(RazorpayWebhookDto dto) {
        return objectMapper.convertValue(dto.getPayload(), JsonNode.class);
    }

    /** Extract payment entity node */
    private JsonNode paymentEntity(RazorpayWebhookDto dto) {
        return getRoot(dto).path("payment").path("entity");
    }

    /** Extract the payment ID (pay_xxx) */
    public String getPaymentId(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("id").asText(null);
    }

    /** Extract the order ID (order_xxx) */
    public String getOrderId(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("order_id").asText(null);
    }

    /** Extract payment status (authorized, captured, failed) */
    public String getPaymentStatus(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("status").asText(null);
    }

    /** Extract amount in paise */
    public Long getAmount(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("amount").asLong();
    }

    /** Extract method (upi, netbanking, card, wallet, etc.) */
    public String getPaymentMethod(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("method").asText(null);
    }

    /** Extract email if present */
    public String getEmail(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("email").asText(null);
    }

    /** Extract contact/mobile */
    public String getContact(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("contact").asText(null);
    }

    /** Extract UPI VPA if available */
    public String getVpa(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("vpa").asText(null);
    }

    /** Extract netbanking bank name */
    public String getBank(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("bank").asText(null);
    }

    /** Extract acquirer bank transaction ID (if any) */
    public String getBankTxnId(RazorpayWebhookDto dto) {
        return paymentEntity(dto).path("acquirer_data").path("bank_transaction_id").asText(null);
    }
}

