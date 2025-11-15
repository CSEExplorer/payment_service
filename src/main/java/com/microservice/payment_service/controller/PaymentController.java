package com.microservice.payment_service.controller;

import com.microservice.payment_service.dto.PaymentRequestDto;
import com.microservice.payment_service.dto.PaymentResponseDto;
import com.microservice.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Capture a payment by externalId
     */
    @PostMapping("/{externalId}/capture")
    public ResponseEntity<PaymentResponseDto> capturePayment(@PathVariable String externalId) {
        PaymentResponseDto response = paymentService.capturePayment(externalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment status by transactionId
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getPayment(@PathVariable Long transactionId) {
        return paymentService.getPayment(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

