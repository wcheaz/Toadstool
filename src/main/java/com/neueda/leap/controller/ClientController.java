package com.neueda.leap.controller;

import com.neueda.leap.Client;
import com.neueda.leap.dto.ClientResponse;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Client endpoints
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * GET /api/clients/{clientId}
     * Retrieve a single client's details
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID clientId) {
        Client client = clientService.getClientById(clientId);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToClientResponse(client));
    }

    /**
     * GET /api/clients
     * List all clients (admin-only endpoint)
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ClientResponse>> listClients(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<Client> clients;
            int totalCount;

            if (status != null && !status.isEmpty()) {
                clients = clientService.listClientsByStatus(status, limit, offset);
                totalCount = clientService.countClientsByStatus(status);
            } else {
                clients = clientService.listAllClients(limit, offset);
                totalCount = clientService.countAllClients();
            }

            List<ClientResponse> responses = clients.stream()
                    .map(this::mapToClientResponse)
                    .collect(Collectors.toList());

            PaginatedResponse<ClientResponse> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/clients/{clientId}
     * Update a client's profile
     */
    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String displayName) {

        try {
            Client client = clientService.getClientById(clientId);
            if (client == null) {
                return ResponseEntity.notFound().build();
            }

            if (displayName != null && !displayName.isEmpty()) {
                clientService.updateClientProfile(clientId, displayName);
                // Refresh client from database
                client = clientService.getClientById(clientId);
            }

            return ResponseEntity.ok(mapToClientResponse(client));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map Client domain object to ClientResponse DTO
     */
    private ClientResponse mapToClientResponse(Client client) {
        return new ClientResponse(
                client.getClientId(),
                client.getEmail(),
                client.getDisplayName(),
                client.getStatus() != null ? client.getStatus().toString() : "ACTIVE",
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
