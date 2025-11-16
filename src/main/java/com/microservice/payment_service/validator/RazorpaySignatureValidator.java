package com.microservice.payment_service.validator;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class RazorpaySignatureValidator {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public boolean isValid(String body, String headerSignature) {
        try {
            String computed = hmacSha256(body, webhookSecret);

            // DEBUG LOGS (remove these in production)
            System.out.println("---- SIGNATURE DEBUG ----");
            System.out.println("RAW BODY:      " + body);
            System.out.println("HEADER SIGN:   " + headerSignature);
            System.out.println("COMPUTED SIGN: " + computed);
            System.out.println("--------------------------");

            return slowEquals(computed, headerSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        sha256_HMAC.init(secretKeySpec);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash); // lowercase hex
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // constant-time comparison
    private boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
