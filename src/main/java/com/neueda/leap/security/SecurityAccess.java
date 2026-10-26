package com.neueda.leap.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.UUID;

public final class SecurityAccess {

    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    private SecurityAccess() {
    }

    public static boolean hasRole(String role) {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }

        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(role::equals);
    }

    public static boolean canAccessClient(UUID clientId) {
        if (hasRole(ADMIN_ROLE)) {
            return true;
        }

        AuthenticatedUser user = currentUser();
        return user != null && clientId != null && clientId.equals(user.getClientId());
    }

    public static boolean canAccessAccount(UUID accountId, UUID ownerClientId) {
        if (hasRole(ADMIN_ROLE)) {
            return true;
        }

        AuthenticatedUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (accountId != null && accountId.equals(user.getAccountId())) {
            return true;
        }
        return ownerClientId != null && ownerClientId.equals(user.getClientId());
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return null;
        }
        if (!authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        return null;
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
