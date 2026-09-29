package com.neueda.leap.controller;

import com.neueda.leap.service.FauxnanceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fauxnance")
public class FauxnanceTestController {
    private final FauxnanceClient fauxnanceClient;

    public FauxnanceTestController(FauxnanceClient fauxnanceClient) {
        this.fauxnanceClient = fauxnanceClient;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        String result = fauxnanceClient.testConnection();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/instruments")
    public ResponseEntity<String> getInstruments() {
        return fauxnanceClient.getInstruments();
    }
}
