package com.microservice.payment_service.entity;


public enum OperationType {
    CREATE_PAYMENT,
    AUTHORIZATION,
    CAPTURE_PAYMENT,
    REFUND,
    VOID,
    OTHER
}

