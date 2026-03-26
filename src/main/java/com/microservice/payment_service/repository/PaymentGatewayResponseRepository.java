package com.microservice.payment_service.repository;


import com.microservice.payment_service.entity.Gateway;
import com.microservice.payment_service.entity.PaymentGatewayResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentGatewayResponseRepository
        extends JpaRepository<PaymentGatewayResponse, Long> {

    // Optional custom queries (only if needed)

    List<PaymentGatewayResponse> findByGateway(Gateway gateway);

    List<PaymentGatewayResponse> findByStatusCode(Integer statusCode);
}