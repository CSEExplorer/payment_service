package com.microservice.payment_service.outbox.model;




public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}