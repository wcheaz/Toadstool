package com.neueda.leap.auth;

import com.neueda.leap.Account;
import com.neueda.leap.AdminUser;
import com.neueda.leap.Client;
import com.neueda.leap.enums.AdminRole;
import com.neueda.leap.enums.AdminUserStatus;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.mapper.AccountMapper;
import com.neueda.leap.auth.AdminCredentialMapper;
import com.neueda.leap.mapper.AdminUserMapper;
import com.neueda.leap.mapper.ClientMapper;
import com.neueda.leap.mapper.LoginEventMapper;
import com.neueda.leap.security.JwtTokenService;
import com.neueda.leap.security.JwtTokenValidator;
import com.neueda.leap.security.ValidatedToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRefreshTest {

    @Mock private ClientMapper clientMapper;
    @Mock private AccountMapper accountMapper;
    @Mock private AdminUserMapper adminUserMapper;
    @Mock private AuthCredentialMapper authCredentialMapper;
    @Mock private AdminCredentialMapper adminCredentialMapper;
    @Mock private LoginEventMapper loginEventMapper;
    @Mock private SessionKeyService sessionKeyService;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private JwtTokenValidator jwtTokenValidator;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                clientMapper,
                accountMapper,
                adminUserMapper,
                authCredentialMapper,
                adminCredentialMapper,
                loginEventMapper,
                sessionKeyService,
                jwtTokenService,
                jwtTokenValidator,
                passwordEncoder
        );
    }

    @Test
    void refreshRotatesClientTokens() {
        UUID sessionKeyId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String refreshToken = "refresh-token";

        Client client = new Client();
        client.setClientId(clientId);
        client.setDisplayName("Test Client");
        client.setEmail("client@example.com");
        client.setStatus(ClientStatus.ACTIVE);

        AuthSession session = new AuthSession();
        session.setSessionKeyId(sessionKeyId);
        session.setClientId(clientId);
        session.setRefreshTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15));

        ValidatedToken validatedToken = ValidatedToken.authenticated(
                clientId.toString(),
                "testuser",
                "client@example.com",
                "ROLE_CLIENT",
                Instant.now(),
                sessionKeyId.toString(),
                "REFRESH",
                clientId.toString(),
                accountId.toString()
        );

        when(jwtTokenValidator.validateToken(refreshToken)).thenReturn(validatedToken);
        when(sessionKeyService.getActiveSessionByRefreshTokenHash(any())).thenReturn(session);
        when(sessionKeyService.rotateSessionTokens(eq(sessionKeyId), any(), any(), any())).thenReturn(true);
        when(clientMapper.selectClientById(clientId)).thenReturn(client);
        when(accountMapper.selectAccountsByClientId(clientId, 1, 0)).thenReturn(List.of(new Account()));
        when(jwtTokenService.issueTokenPair(
                eq(sessionKeyId),
                eq(clientId),
                eq(clientId),
                eq(accountId),
                eq("testuser"),
                eq("client@example.com"),
                eq("CLIENT")
        )).thenReturn(new JwtTokenService.IssuedTokenPair(
                "new-access-token",
                Instant.now().plus(Duration.ofMinutes(15)),
                "new-refresh-token",
                Instant.now().plus(Duration.ofMinutes(15))
        ));

        AuthResponse response = authService.refresh(tokenRequest(refreshToken), null);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("Test Client", response.getDisplayName());
        assertEquals("CLIENT", response.getRole());
        verify(sessionKeyService).rotateSessionTokens(eq(sessionKeyId), any(), any(), any());
    }

    @Test
    void refreshIssuesAdminTokensWithoutSessionRotation() {
        UUID adminUserId = UUID.randomUUID();
        String refreshToken = "admin-refresh-token";

        AdminUser adminUser = new AdminUser();
        adminUser.setAdminUserId(adminUserId);
        adminUser.setEmail("admin@example.com");
        adminUser.setDisplayName("Admin User");
        adminUser.setRole(AdminRole.ADMIN);
        adminUser.setStatus(AdminUserStatus.ACTIVE);

        ValidatedToken validatedToken = ValidatedToken.authenticated(
                adminUserId.toString(),
                "admin@example.com",
                "admin@example.com",
                "ROLE_ADMIN",
                Instant.now(),
                null,
                "REFRESH",
                null,
                null
        );

        when(jwtTokenValidator.validateToken(refreshToken)).thenReturn(validatedToken);
        when(adminUserMapper.selectActiveAdminUserByEmail("admin@example.com")).thenReturn(adminUser);
        when(jwtTokenService.issueTokenPair(
                eq(null),
                eq(adminUserId),
                eq(null),
                eq(null),
                eq("admin@example.com"),
                eq("admin@example.com"),
                eq("ADMIN")
        )).thenReturn(new JwtTokenService.IssuedTokenPair(
                "admin-access-token",
                Instant.now().plus(Duration.ofMinutes(15)),
                "admin-refresh-token-2",
                Instant.now().plus(Duration.ofMinutes(15))
        ));

        AuthResponse response = authService.refresh(tokenRequest(refreshToken), null);

        assertNotNull(response);
        assertEquals("admin-access-token", response.getAccessToken());
        assertEquals("admin-refresh-token-2", response.getRefreshToken());
        assertEquals("Admin User", response.getDisplayName());
        assertEquals("ADMIN", response.getRole());
    }

    private TokenValidationRequest tokenRequest(String token) {
        TokenValidationRequest request = new TokenValidationRequest();
        request.setToken(token);
        return request;
    }
}
