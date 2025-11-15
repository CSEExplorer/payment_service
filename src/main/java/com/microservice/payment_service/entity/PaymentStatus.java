package com.microservice.payment_service.entity;


public enum PaymentStatus {
    PARTIALLY_REFUNDED,
    CREATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    CANCELLED,
    PENDING
}
