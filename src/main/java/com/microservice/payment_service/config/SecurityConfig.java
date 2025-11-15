package com.microservice.payment_service.config;

import com.microservice.payment_service.filter.RazorpaySignatureFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final RazorpaySignatureFilter razorpaySignatureFilter;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webhook/**").permitAll()   // Webhooks must be public
                        .anyRequest().permitAll()
                )
                .addFilterBefore(razorpaySignatureFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

