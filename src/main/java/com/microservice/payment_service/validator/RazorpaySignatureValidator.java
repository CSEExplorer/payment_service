package com.microservice.payment_service.validator;


import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class RazorpaySignatureValidator {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public boolean isValid(String body, String signature) {
        if (signature == null) return false;

        try {
            String generated = hmacSha256(body, webhookSecret);
            return generated.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("H macSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "H macSHA256");
        sha256_HMAC.init(keySpec);

        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); // hex lowercase
        }
        return sb.toString();
    }

}

