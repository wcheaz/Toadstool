package com.neueda.leap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "{\n  \"status\": \"UP\",\n  \"message\": \"Trading Platform API is running\"\n}";
    }
}
