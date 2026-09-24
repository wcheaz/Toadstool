package com.neueda.leap.controller;

import com.neueda.leap.LoginEvent;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.service.LoginEventService;
import com.neueda.leap.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for LoginEvent endpoints (security audit)
 */
@RestController
@RequestMapping("/api")
public class LoginEventController {

    private final LoginEventService loginEventService;
    private final ClientService clientService;

    public LoginEventController(LoginEventService loginEventService, ClientService clientService) {
        this.loginEventService = loginEventService;
        this.clientService = clientService;
    }

    /**
     * GET /api/clients/{clientId}/login-events
     * Get login attempt history for a client
     */
    @GetMapping("/clients/{clientId}/login-events")
    public ResponseEntity<PaginatedResponse<LoginEventDto>> listLoginEventsByClient(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String outcome,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Verify client exists
            if (clientService.getClientById(clientId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<LoginEvent> events = loginEventService.listLoginEventsByClient(clientId, limit, offset);
            int totalCount = loginEventService.countLoginEventsByClient(clientId);

            List<LoginEventDto> eventDtos = events.stream()
                    .map(this::mapToLoginEventDto)
                    .collect(Collectors.toList());

            PaginatedResponse<LoginEventDto> pagedResponse = new PaginatedResponse<>(eventDtos, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/login-events
     * Get all login events (admin-only, security analysis)
     */
    @GetMapping("/admin/login-events")
    public ResponseEntity<PaginatedResponse<LoginEventDto>> listLoginEventsAdmin(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String emailAttempted,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<LoginEvent> events = loginEventService.listAllLoginEvents(limit, offset);
            int totalCount = loginEventService.countAllLoginEvents();

            List<LoginEventDto> eventDtos = events.stream()
                    .map(this::mapToLoginEventDto)
                    .collect(Collectors.toList());

            PaginatedResponse<LoginEventDto> pagedResponse = new PaginatedResponse<>(eventDtos, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map LoginEvent domain object to LoginEventDto
     */
    private LoginEventDto mapToLoginEventDto(LoginEvent event) {
        String outcomeStr = event.getOutcome() != null ? event.getOutcome().toString() : "FAILURE";
        
        return new LoginEventDto(
                event.getLoginEventId(),
                event.getClientId(),
                event.getEmailAttempted(),
                outcomeStr,
                event.getOccurredAt(),
                event.getDetails()
        );
    }

    /**
     * DTO for LoginEvent response
     */
    public static class LoginEventDto {
        private UUID loginEventId;
        private UUID clientId;
        private String emailAttempted;
        private String outcome;
        private OffsetDateTime occurredAt;
        private String details;

        public LoginEventDto(UUID loginEventId, UUID clientId, String emailAttempted, 
                            String outcome, OffsetDateTime occurredAt, String details) {
            this.loginEventId = loginEventId;
            this.clientId = clientId;
            this.emailAttempted = emailAttempted;
            this.outcome = outcome;
            this.occurredAt = occurredAt;
            this.details = details;
        }

        // Getters
        public UUID getLoginEventId() { return loginEventId; }
        public UUID getClientId() { return clientId; }
        public String getEmailAttempted() { return emailAttempted; }
        public String getOutcome() { return outcome; }
        public OffsetDateTime getOccurredAt() { return occurredAt; }
        public String getDetails() { return details; }
    }
}
