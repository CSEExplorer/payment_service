package com.microservice.payment_service.config;


import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.Base64;

public class RazorpayFeignConfig {

    @Value("${gateway.razorpay.key_id}")
    private String keyId;

    @Value("${gateway.razorpay.secret}")
    private String keySecret;

    @Bean
    public RequestInterceptor razorpayAuthInterceptor() {
        return template -> {
            String auth = keyId + ":" + keySecret;
            String base64 = Base64.getEncoder().encodeToString(auth.getBytes());
            template.header("Authorization", "Basic " + base64);
            template.header("Content-Type", "application/json");
        };
    }
}
