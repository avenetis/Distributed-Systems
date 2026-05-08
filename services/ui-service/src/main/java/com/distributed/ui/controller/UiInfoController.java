package com.distributed.ui.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UiInfoController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "service", "ui-service",
                "status", "running",
                "health", "/health",
                "jobs", "/jobs"
        );
    }
}
