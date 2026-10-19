package com.neueda.leap.controller;

import com.neueda.leap.dto.AdminDtos.AccessEvent;
import com.neueda.leap.dto.AdminDtos.LoginEventRow;
import com.neueda.leap.dto.AdminDtos.TradeEventRow;
import com.neueda.leap.dto.AdminDtos.CashEventRow;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.service.AdminOversightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Platform-wide audit trail for admins: sign-ins, trade events and admin data access.
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AdminOversightService adminOversightService;

    public AdminAuditController(AdminOversightService adminOversightService) {
        this.adminOversightService = adminOversightService;
    }

    @GetMapping("/login-events")
    public ResponseEntity<PaginatedResponse<LoginEventRow>> listLoginEvents(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String emailAttempted,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        ResponseEntity<PaginatedResponse<LoginEventRow>> rejected = reject(limit, offset);
        if (rejected != null) {
            return rejected;
        }
        List<LoginEventRow> events = adminOversightService.listLoginEvents(clientId, outcome, emailAttempted, limit, offset);
        int totalCount = adminOversightService.countLoginEvents(clientId, outcome, emailAttempted);
        return ResponseEntity.ok(new PaginatedResponse<>(events, limit, offset, totalCount));
    }

    @GetMapping("/trade-events")
    public ResponseEntity<PaginatedResponse<TradeEventRow>> listTradeEvents(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        ResponseEntity<PaginatedResponse<TradeEventRow>> rejected = reject(limit, offset);
        if (rejected != null) {
            return rejected;
        }
        List<TradeEventRow> events = adminOversightService.listTradeEvents(clientId, action, limit, offset);
        int totalCount = adminOversightService.countTradeEvents(clientId, action);
        return ResponseEntity.ok(new PaginatedResponse<>(events, limit, offset, totalCount));
    }

    @GetMapping("/access-events")
    public ResponseEntity<PaginatedResponse<AccessEvent>> listAccessEvents(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        ResponseEntity<PaginatedResponse<AccessEvent>> rejected = reject(limit, offset);
        if (rejected != null) {
            return rejected;
        }
        List<AccessEvent> events = adminOversightService.listAccessEvents(clientId, limit, offset);
        int totalCount = adminOversightService.countAccessEvents(clientId);
        return ResponseEntity.ok(new PaginatedResponse<>(events, limit, offset, totalCount));
    }

    @GetMapping("/cash-events")
    public ResponseEntity<PaginatedResponse<CashEventRow>> listCashEvents(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        ResponseEntity<PaginatedResponse<CashEventRow>> rejected = reject(limit, offset);
        if (rejected != null) {
            return rejected;
        }
        List<CashEventRow> events = adminOversightService.listCashEvents(clientId, eventType, limit, offset);
        int totalCount = adminOversightService.countCashEvents(clientId, eventType);
        return ResponseEntity.ok(new PaginatedResponse<>(events, limit, offset, totalCount));
    }

    private static <T> ResponseEntity<T> reject(int limit, int offset) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (limit < 1 || limit > 500 || offset < 0) {
            return ResponseEntity.badRequest().build();
        }
        return null;
    }
}
