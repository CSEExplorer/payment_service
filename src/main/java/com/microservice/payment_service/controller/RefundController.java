package com.microservice.payment_service.controller;


import com.microservice.payment_service.dto.RefundRequestDto;
import com.microservice.payment_service.entity.Refund;
import com.microservice.payment_service.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * Initiate a refund for a transaction
     */
    @PostMapping
    public ResponseEntity<Refund> initiateRefund(@RequestBody RefundRequestDto request) {
        Refund refund = refundService.initiateRefund(request);
        return ResponseEntity.ok(refund);
    }

    /**
     * Get refund details by refundId
     */
    @GetMapping("/{refundId}")
    public ResponseEntity<?> getRefund(@PathVariable String refundId) {
        return ResponseEntity.ok("Not implemented yet - optional");
    }
}

