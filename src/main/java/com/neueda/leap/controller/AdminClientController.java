package com.neueda.leap.controller;

import com.neueda.leap.dto.AdminDtos.ClientDetail;
import com.neueda.leap.dto.AdminDtos.ClientSummary;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.service.AdminOversightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin client oversight: search clients and open a client's account overview.
 * Per-account data (holdings, orders, fills, cash events) comes from the regular
 * account endpoints, which admins are allowed to read.
 */
@RestController
@RequestMapping("/api/admin/clients")
public class AdminClientController {

    private final AdminOversightService adminOversightService;

    public AdminClientController(AdminOversightService adminOversightService) {
        this.adminOversightService = adminOversightService;
    }

    /**
     * GET /api/admin/clients?q=&status=&limit=&offset=
     * Search clients by name, email, username or client id prefix.
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<ClientSummary>> searchClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (limit < 1 || limit > 200 || offset < 0) {
            return ResponseEntity.badRequest().build();
        }
        List<ClientSummary> clients = adminOversightService.searchClients(q, status, limit, offset);
        int totalCount = adminOversightService.countClients(q, status);
        return ResponseEntity.ok(new PaginatedResponse<>(clients, limit, offset, totalCount));
    }

    /**
     * GET /api/admin/clients/{clientId}
     * Client header and accounts. Each call is recorded in the admin access log.
     */
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientDetail> getClient(@PathVariable UUID clientId) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ClientDetail detail = adminOversightService.viewClient(clientId);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }
}
