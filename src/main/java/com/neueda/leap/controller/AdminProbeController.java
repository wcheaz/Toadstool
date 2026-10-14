package com.neueda.leap.controller;

import com.neueda.leap.security.AuthenticatedUser;
import com.neueda.leap.security.SecurityAccess;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal admin-only endpoint used to verify role-based access control.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminProbeController {

    @GetMapping("/probe")
    public ResponseEntity<Map<String, Object>> probe() {
        AuthenticatedUser user = SecurityAccess.currentUser();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("message", "Admin probe reached");
        response.put("subject", user != null ? user.getSubject() : null);
        response.put("username", user != null ? user.getUsername() : null);

        return ResponseEntity.ok(response);
    }
}
