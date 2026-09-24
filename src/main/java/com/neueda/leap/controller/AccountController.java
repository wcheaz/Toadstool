package com.neueda.leap.controller;

import com.neueda.leap.Account;
import com.neueda.leap.dto.AccountResponse;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.dto.HoldingsResponse;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Account endpoints
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;
    private final ClientService clientService;

    public AccountController(AccountService accountService, ClientService clientService) {
        this.accountService = accountService;
        this.clientService = clientService;
    }

    /**
     * POST /api/accounts
     * Create a new trading account for a client
     */
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        try {
            if (request == null || request.getClientId() == null) {
                return ResponseEntity.badRequest().build();
            }

            UUID clientId = request.getClientId();

            // Verify client exists
            if (clientService.getClientById(clientId) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Account account = accountService.createAccount(clientId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapToAccountResponse(account));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/accounts/{accountId}
     * Retrieve account details
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        Account account = accountService.getAccountById(accountId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToAccountResponse(account));
    }

    /**
     * GET /api/clients/{clientId}/accounts
     * List accounts for a client
     */
    @GetMapping("/clients/{clientId}/accounts")
    public ResponseEntity<PaginatedResponse<AccountResponse>> listAccountsByClient(
            @PathVariable UUID clientId,
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

            List<Account> accounts = accountService.listAccountsByClient(clientId, limit, offset);
            int totalCount = accountService.countAccountsByClient(clientId);

            List<AccountResponse> responses = accounts.stream()
                    .map(this::mapToAccountResponse)
                    .collect(Collectors.toList());

            PaginatedResponse<AccountResponse> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/accounts/{accountId}/holdings
     * Get current positions and cash balance
     * (Stub implementation - returns empty holdings for now)
     */
    @GetMapping("/accounts/{accountId}/holdings")
    public ResponseEntity<HoldingsResponse> getHoldings(@PathVariable UUID accountId) {
        try {
            Account account = accountService.getAccountById(accountId);
            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            // Stub: return empty holdings
            HoldingsResponse response = new HoldingsResponse(
                    accountId,
                    List.of(),  // Empty holdings list
                    new HoldingsResponse.CashBalance("0.0000000000", "USD"),
                    OffsetDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map Account domain object to AccountResponse DTO
     */
    private AccountResponse mapToAccountResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getClientId(),
                account.getStatus() != null ? account.getStatus().toString() : "ACTIVE",
                account.getOpenedAt()
        );
    }

    public static class CreateAccountRequest {
        private UUID clientId;

        public UUID getClientId() {
            return clientId;
        }

        public void setClientId(UUID clientId) {
            this.clientId = clientId;
        }
    }
}
