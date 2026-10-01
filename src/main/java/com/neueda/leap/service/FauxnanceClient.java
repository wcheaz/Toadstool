package com.neueda.leap.service;

import com.neueda.leap.config.FauxnanceProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FauxnanceClient {
    private final RestTemplate restTemplate;
    private final FauxnanceProperties properties;

    public FauxnanceClient(RestTemplate restTemplate, FauxnanceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String testConnection() {
        try {
            String url = properties.getBaseUrl() + "/markets/instruments";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getApiKey());
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return "Status: " + response.getStatusCode() + "\nData: " + response.getBody();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public ResponseEntity<String> getInstruments() {
        try {
            String url = properties.getBaseUrl() + "/markets/instruments";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getApiKey());
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
