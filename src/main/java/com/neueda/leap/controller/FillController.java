package com.neueda.leap.controller;

import com.neueda.leap.Fill;
import com.neueda.leap.dto.FillResponse;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.service.FillService;
import com.neueda.leap.service.OrderService;
import com.neueda.leap.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Fill endpoints (executed trades)
 */
@RestController
@RequestMapping("/api")
public class FillController {

    private final FillService fillService;
    private final OrderService orderService;
    private final AccountService accountService;

    public FillController(FillService fillService, OrderService orderService, AccountService accountService) {
        this.fillService = fillService;
        this.orderService = orderService;
        this.accountService = accountService;
    }

    /**
     * GET /api/fills/{fillId}
     * Retrieve a single fill
     */
    @GetMapping("/fills/{fillId}")
    public ResponseEntity<FillResponse> getFill(@PathVariable UUID fillId) {
        Fill fill = fillService.getFillById(fillId);
        if (fill == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToFillResponse(fill));
    }

    /**
     * GET /api/orders/{orderId}/fills
     * List all fills for an order
     */
    @GetMapping("/orders/{orderId}/fills")
    public ResponseEntity<PaginatedResponse<FillResponse>> listFillsByOrder(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Verify order exists
            if (orderService.getOrderById(orderId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<Fill> fills = fillService.listFillsByOrderPaginated(orderId, limit, offset);
            int totalCount = fillService.countFillsByOrder(orderId);

            List<FillResponse> responses = fills.stream()
                    .map(this::mapToFillResponse)
                    .collect(Collectors.toList());

            PaginatedResponse<FillResponse> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/accounts/{accountId}/fills
     * List all fills for an account
     */
    @GetMapping("/accounts/{accountId}/fills")
    public ResponseEntity<PaginatedResponse<FillResponse>> listFillsByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Verify account exists
            if (accountService.getAccountById(accountId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<Fill> fills = fillService.listFillsByAccount(accountId, limit, offset);
            int totalCount = fillService.countFillsByAccount(accountId);

            List<FillResponse> responses = fills.stream()
                    .map(this::mapToFillResponse)
                    .collect(Collectors.toList());

            PaginatedResponse<FillResponse> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map Fill domain object to FillResponse DTO
     */
    private FillResponse mapToFillResponse(Fill fill) {
        return new FillResponse(
                fill.getFillId(),
                fill.getOrderId(),
                fill.getPrice().toPlainString(),
                fill.getQuantity().toPlainString(),
                fill.getStatus() != null ? fill.getStatus().toString() : "Pending",
                fill.getExecutedAt()
        );
    }
}
