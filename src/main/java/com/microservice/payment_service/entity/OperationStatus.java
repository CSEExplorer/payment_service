package com.microservice.payment_service.entity;


public enum OperationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    RETRYABLE,
    CANCELLED
}

