package com.microservice.payment_service.service;

import com.microservice.payment_service.adapter.GatewayAdapter;
import com.microservice.payment_service.dto.RefundRequestDto;
import com.microservice.payment_service.entity.*;
import com.microservice.payment_service.repository.PaymentTransactionRepository;
import com.microservice.payment_service.repository.RefundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final GatewayAdapterService gatewayAdapterService;

    @Transactional
    public Refund initiateRefund(RefundRequestDto request) {
        log.info("Initiating refund for transactionId={} amount={}", request.getTransactionId(), request.getAmount());

        PaymentTransaction tx = paymentTransactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getStatus() != PaymentStatus.CAPTURED && tx.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Only captured payments can be refunded.");
        }

        BigDecimal totalRefunded = tx.getRefundedAmount().add(request.getAmount());
        if (totalRefunded.compareTo(tx.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds original payment.");
        }

        // Create refund entity
        Refund refund = Refund.builder()
                .paymentTransaction(tx)
                .amount(request.getAmount())
                .currency(tx.getCurrency())
                .reason(request.getReason())
                .status(RefundStatus.PROCESSING)
                .build();

        refundRepository.save(refund);

        try {
            GatewayAdapter adapter = gatewayAdapterService.getAdapter(tx.getGateway());
            PaymentGatewayResponse gatewayResponse = adapter.initiateRefund(tx, refund);
            refund.setGatewayResponse(gatewayResponse);
            refund.setStatus(RefundStatus.COMPLETED);
            refundRepository.save(refund);

            tx.setRefundedAmount(totalRefunded);
            if (totalRefunded.compareTo(tx.getAmount()) == 0) {
                tx.setStatus(PaymentStatus.REFUNDED);
            } else {
                tx.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            paymentTransactionRepository.save(tx);

            return refund;
        } catch (Exception ex) {
            refund.setStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
            log.error("Refund failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Refund failed: " + ex.getMessage(), ex);
        }
    }
}

