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
        return loginEventMapper.selectAllLoginEvents(limit, offset);
    }

    /**
     * Count all login events
     */
    public int countAllLoginEvents() {
        return loginEventMapper.countAllLoginEvents();
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
