package com.microservice.payment_service.entity;

/**
 * Simple enum to identify which gateway handled the transaction.
 * Extend as you add more adapters.
 */
public enum Gateway {
    RAZORPAY,
    STRIPE,
    PAYPAL,
    OTHER
}

