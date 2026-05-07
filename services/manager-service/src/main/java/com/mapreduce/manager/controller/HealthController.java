package com.mapreduce.manager.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "manager-service");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    @GetMapping("/ready")
    public Map<String, String> ready() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "READY");
        return response;
    }

}
