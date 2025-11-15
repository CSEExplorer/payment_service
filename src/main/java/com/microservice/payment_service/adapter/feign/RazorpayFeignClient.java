package com.microservice.payment_service.adapter.feign;


import com.microservice.payment_service.config.RazorpayFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(
        name = "razorpayClient",
        url = "${gateway.razorpay.url}",
        configuration = RazorpayFeignConfig.class
)
public interface RazorpayFeignClient {

    @PostMapping("/v1/orders")
    Map<String, Object> createOrder(@RequestBody Map<String, Object> body);

    @PostMapping("/v1/payments/{paymentId}/capture")
    Map<String, Object> capturePayment(@RequestBody Map<String, Object> body);

    @PostMapping("/v1/payments/{paymentId}/refund")
    Map<String, Object> initiateRefund(@RequestBody Map<String, Object> body);
}

