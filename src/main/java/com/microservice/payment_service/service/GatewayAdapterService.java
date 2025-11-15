package com.microservice.payment_service.service;


import com.microservice.payment_service.adapter.GatewayAdapter;
import com.microservice.payment_service.adapter.RazorpayAdapter;
import com.microservice.payment_service.entity.Gateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GatewayAdapterService {

    private final RazorpayAdapter razorpayAdapter;


    private final Map<Gateway, GatewayAdapter> adapterMap = new EnumMap<>(Gateway.class);

    /**
     * Post-constructor to populate the map for quick lookup.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        adapterMap.put(Gateway.RAZORPAY, razorpayAdapter);

    }

    public GatewayAdapter getAdapter(Gateway gateway) {
        GatewayAdapter adapter = adapterMap.get(gateway);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported gateway: " + gateway);
        }
        return adapter;
    }
}

