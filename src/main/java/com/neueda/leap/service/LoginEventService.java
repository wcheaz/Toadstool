package com.neueda.leap.service;

import com.neueda.leap.LoginEvent;
import com.neueda.leap.mapper.LoginEventMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for LoginEvent operations (security audit)
 */
@Service
public class LoginEventService {

    private final LoginEventMapper loginEventMapper;

    public LoginEventService(LoginEventMapper loginEventMapper) {
        this.loginEventMapper = loginEventMapper;
    }

    /**
     * Get login event by ID
     */
    public LoginEvent getLoginEventById(UUID loginEventId) {
        return loginEventMapper.selectLoginEventById(loginEventId);
    }

    /**
     * List login events for a client with pagination
     */
    public List<LoginEvent> listLoginEventsByClient(UUID clientId, int limit, int offset) {
        return loginEventMapper.selectLoginEventsByClientId(clientId, limit, offset);
    }

    /**
     * Count login events for a client
     */
    public int countLoginEventsByClient(UUID clientId) {
        return loginEventMapper.countLoginEventsByClientId(clientId);
    }

    /**
     * List all login events (admin-only) with pagination
     */
    public List<LoginEvent> listAllLoginEvents(int limit, int offset) {
        return listLoginEvents(null, null, null, limit, offset);
    }

    /**
     * Count all login events
     */
    public int countAllLoginEvents() {
        return countLoginEvents(null, null, null);
    }

    /**
     * List login events filtered by client, outcome and/or a partial email; null filters are ignored
     */
    public List<LoginEvent> listLoginEvents(UUID clientId, String outcome, String emailAttempted, int limit, int offset) {
        return loginEventMapper.selectLoginEvents(clientId, blankToNull(outcome), blankToNull(emailAttempted), limit, offset);
    }

    public int countLoginEvents(UUID clientId, String outcome, String emailAttempted) {
        return loginEventMapper.countLoginEvents(clientId, blankToNull(outcome), blankToNull(emailAttempted));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Record a new login event
     */
    public void recordLoginEvent(UUID clientId, String emailAttempted, String outcome, String details) {
        validateLoginEvent(emailAttempted, outcome);
        loginEventMapper.insertLoginEvent(clientId, emailAttempted, outcome, details);
    }

    /**
     * Record successful login
     */
    public void recordLoginSuccess(UUID clientId, String email) {
        recordLoginEvent(clientId, email, "SUCCESS", null);
    }

    /**
     * Record failed login attempt
     */
    public void recordLoginFailure(String email, String reason) {
        recordLoginEvent(null, email, "FAILURE", reason);
    }

    /**
     * Validate login event input
     */
    private void validateLoginEvent(String emailAttempted, String outcome) {
        if (emailAttempted == null || emailAttempted.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (outcome == null || (!outcome.equals("SUCCESS") && !outcome.equals("FAILURE"))) {
            throw new IllegalArgumentException("Outcome must be SUCCESS or FAILURE");
        }
    }
}
