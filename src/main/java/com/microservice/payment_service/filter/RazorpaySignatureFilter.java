package com.microservice.payment_service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.payment_service.utility.CachedBodyHttpServletRequest;
import com.microservice.payment_service.validator.RazorpaySignatureValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RazorpaySignatureFilter extends OncePerRequestFilter {

    private final RazorpaySignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only protect Razorpay webhook endpoint
        if (path.contains("/api/webhook/razorpay")) {

            String signature = request.getHeader("X-Razorpay-Signature");

            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);


            if (!signatureValidator.isValid(body, signature)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid webhook signature");
                    return; // Stop filter chain
                }

            // Wrap body again for controller usage
            HttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);
            filterChain.doFilter(wrapped, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

