package com.neueda.leap.controller;

import com.neueda.leap.Client;
import com.neueda.leap.dto.LoginRequest;
import com.neueda.leap.dto.LoginResponse;
import com.neueda.leap.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Login endpoints
 */
@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "*")
public class LoginController {

    private final ClientService clientService;

    public LoginController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * POST /api/login
     * Verify client email against the clients table
     * 
     * Request body: { "email": "user@example.com" }
     * Returns 200 with client details if email is found
     * Returns 401 if email is not found in clients table
     */
    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Validate email input
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Email is required"));
        }

        // Query clients table by email
        Client client = clientService.getClientByEmail(loginRequest.getEmail());

        if (client == null) {
            // Email not found in clients table
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Email not found in system"));
        }

        // Email found, return client details
        LoginResponse response = new LoginResponse(
                client.getClientId(),
                client.getEmail(),
                client.getDisplayName(),
                client.getStatus().toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Simple error response class
     */
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
