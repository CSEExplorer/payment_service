package com.microservice.payment_service.dto.callback;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RazorpayWebhookDto {
    private String entity;           // "event"
    private String account_id;
    private String event;            // "payment.authorized"
    private List<String> contains;   // ["payment"]
    private Map<String, Object> payload;  // Dynamic
    private long created_at;
}

