package com.neueda.leap.auth;

import com.neueda.leap.Account;
import com.neueda.leap.AdminUser;
import com.neueda.leap.Client;
import com.neueda.leap.enums.AdminRole;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.enums.AdminUserStatus;
import com.neueda.leap.mapper.AccountMapper;
import com.neueda.leap.mapper.AdminUserMapper;
import com.neueda.leap.mapper.ClientMapper;
import com.neueda.leap.mapper.LoginEventMapper;
import com.neueda.leap.security.JwtAuthenticationFilter;
import com.neueda.leap.security.JwtTokenService.IssuedAccessToken;
import com.neueda.leap.security.JwtTokenService;
import com.neueda.leap.security.JwtTokenValidator;
import com.neueda.leap.security.ValidatedToken;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final ClientMapper clientMapper;
    private final AccountMapper accountMapper;
    private final AdminUserMapper adminUserMapper;
    private final AuthCredentialMapper authCredentialMapper;
    private final LoginEventMapper loginEventMapper;
    private final SessionKeyService sessionKeyService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenValidator jwtTokenValidator;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;


    public AuthService(ClientMapper clientMapper, AccountMapper accountMapper, AdminUserMapper adminUserMapper,
                       AuthCredentialMapper authCredentialMapper, LoginEventMapper loginEventMapper, 
                       SessionKeyService sessionKeyService, JwtTokenService jwtTokenService,
                       JwtTokenValidator jwtTokenValidator, PasswordEncoder passwordEncoder) {
        this.clientMapper = clientMapper;
        this.accountMapper = accountMapper;
        this.adminUserMapper = adminUserMapper;
        this.authCredentialMapper = authCredentialMapper;
        this.loginEventMapper = loginEventMapper;
        this.sessionKeyService = sessionKeyService;
        this.jwtTokenService = jwtTokenService;
        this.jwtTokenValidator = jwtTokenValidator;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        AuthRegisterRequest normalizedRequest = normalizeRegisterRequest(request);
        validateRegisterRequest(normalizedRequest);
        if (clientMapper.selectClientByEmail(normalizedRequest.getEmail()) != null) {
            recordLoginEvent(null, normalizedRequest.getEmail(), "FAILURE", "Email already registered");
            throw new AuthApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (authCredentialMapper.selectByUsername(normalizedRequest.getUsername()) != null) {
            recordLoginEvent(null, normalizedRequest.getEmail(), "FAILURE", "Username already registered");
            throw new AuthApiException(HttpStatus.CONFLICT, "Username is already registered");
        }

        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID credentialId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode(normalizedRequest.getPassword());

        clientMapper.insertClient(clientId, normalizedRequest.getEmail(), normalizedRequest.getDisplayName(), ClientStatus.ACTIVE.name());
        accountMapper.insertAccountWithId(accountId, clientId, AccountStatus.ACTIVE.name());
        authCredentialMapper.insertCredential(credentialId, clientId, normalizedRequest.getUsername(), encodedPassword);

        Client client = new Client();
        client.setClientId(clientId);
        client.setEmail(normalizedRequest.getEmail());
        client.setDisplayName(normalizedRequest.getDisplayName());
        client.setStatus(ClientStatus.ACTIVE);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setClientId(clientId);
        account.setStatus(AccountStatus.ACTIVE);

        recordLoginEvent(clientId, normalizedRequest.getEmail(), "SUCCESS", "Registration successful");
        return issueAuthResponse(client, account, normalizedRequest.getUsername(), credentialId);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        AuthLoginRequest normalizedRequest = normalizeLoginRequest(request);
        validateLoginRequest(normalizedRequest);
        AuthCredential credential = authCredentialMapper.selectByUsername(normalizedRequest.getUsername());
        if (credential == null || !passwordEncoder.matches(normalizedRequest.getPassword(), credential.getPasswordHash())) {
            Client attemptedClient = credential != null ? clientMapper.selectClientById(credential.getClientId()) : null;
            recordLoginEvent(attemptedClient != null ? attemptedClient.getClientId() : null,
                           attemptedClient != null ? attemptedClient.getEmail() : normalizedRequest.getUsername(),
                           "FAILURE", "Invalid username or password");
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        authCredentialMapper.markLastLogin(credential.getCredentialId());
        Client client = clientMapper.selectClientById(credential.getClientId());
        List<Account> accounts = accountMapper.selectAccountsByClientId(credential.getClientId(), 1, 0);
        if (accounts.isEmpty()) {
            recordLoginEvent(client.getClientId(), client.getEmail(), "FAILURE", "Account not found for credential");
            throw new IllegalStateException("Account not found for credential");
        }
        Account account = accounts.get(0);
        recordLoginEvent(client.getClientId(), client.getEmail(), "SUCCESS", null);
        return issueAuthResponse(client, account, credential.getUsername(), credential.getCredentialId());
    }

    public AuthResponse adminLogin(AdminLoginRequest request) {
        AdminLoginRequest normalizedRequest = normalizeAdminLoginRequest(request);
        validateAdminLoginRequest(normalizedRequest);

        AdminUser adminUser = adminUserMapper.selectActiveAdminUserByEmail(normalizedRequest.getEmail());
        if (adminUser == null || !passwordEncoder.matches(normalizedRequest.getPassword(), adminUser.getPasswordHash())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid admin email or password");
        }

        if (adminUser.getRole() != AdminRole.ADMIN) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        if (adminUser.getStatus() != AdminUserStatus.ACTIVE) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, "Admin user is not active");
        }

        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        IssuedAccessToken issuedAccessToken = jwtTokenService.issueAccessToken(
                null,
                adminUser.getAdminUserId(),
                null,
                adminUser.getEmail(),
                adminUser.getEmail(),
                "ADMIN"
        );

        if (issuedAccessToken.getAccessToken() == null) {
            throw new IllegalStateException("JWT authentication is disabled");
        }

        return new AuthResponse(
                issuedAccessToken.getAccessToken(),
                Duration.between(issuedAt.toInstant(), issuedAccessToken.getAccessTokenExpiresAt()).getSeconds(),
                null,
                null,
                adminUser.getDisplayName(),
                adminUser.getEmail(),
                adminUser.getEmail(),
                "ADMIN"
        );
    }

    private AuthResponse issueAuthResponse(Client client, Account account, String username, UUID credentialId) {
        if (client == null || account == null) {
            throw new IllegalStateException("Client and account are required to issue JWTs");
        }
        if (client.getClientId() == null) {
            throw new IllegalStateException("Client ID is required to issue JWTs");
        }
        if (account.getAccountId() == null) {
            throw new IllegalStateException("Account ID is required to issue JWTs");
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new IllegalStateException("Client email is required to issue JWTs");
        }

        UUID sessionKeyId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        IssuedAccessToken issuedAccessToken = jwtTokenService.issueAccessToken(
                sessionKeyId,
                client.getClientId(),
                account.getAccountId(),
                username,
                client.getEmail(),
                "CLIENT"
        );

        if (issuedAccessToken.getAccessToken() == null) {
            throw new IllegalStateException("JWT authentication is disabled");
        }

        sessionKeyService.createSession(
                sessionKeyId,
                credentialId,
                client.getClientId(),
                issuedAt,
                OffsetDateTime.ofInstant(issuedAccessToken.getAccessTokenExpiresAt(), ZoneOffset.UTC)
        );

        return new AuthResponse(
                issuedAccessToken.getAccessToken(),
                Duration.between(issuedAt.toInstant(), issuedAccessToken.getAccessTokenExpiresAt()).getSeconds(),
                client.getClientId(),
                account.getAccountId(),
                client.getDisplayName(),
                username,
                client.getEmail(),
                "CLIENT"
        );
    }

    private void validateRegisterRequest(AuthRegisterRequest request) {
        if (request == null) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Registration details are required");
        }
        if (!StringUtils.hasText(request.getEmail()) || !request.getEmail().contains("@")) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "A valid email is required");
        }
        if (!StringUtils.hasText(request.getDisplayName()) || request.getDisplayName().trim().length() > 120) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Display name must be 1-120 characters");
        }
        if (!StringUtils.hasText(request.getUsername()) || request.getUsername().trim().length() > 80) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Username must be 1-80 characters");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
    }

    private void validateLoginRequest(AuthLoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
    }

    private void validateAdminLoginRequest(AdminLoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AuthApiException(HttpStatus.BAD_REQUEST, "Admin email and password are required");
        }
    }

    public TokenValidationResponse validateToken(TokenValidationRequest request, String authorizationHeader) {
        String token = extractToken(request, authorizationHeader);
        try {
            ValidatedToken validatedToken = jwtTokenValidator.validateToken(token);
            if (!validatedToken.isAuthenticated()) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT authentication is disabled");
            }
            if (!"ACCESS".equalsIgnoreCase(validatedToken.getTokenType())) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Only access tokens can be validated for API use");
            }
            if (StringUtils.hasText(validatedToken.getTokenId())
                    && !sessionKeyService.isActiveSession(validatedToken.getTokenId())) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT session is no longer active");
            }

            return new TokenValidationResponse(
                    true,
                    validatedToken.getSubject(),
                    validatedToken.getUsername(),
                    validatedToken.getEmail(),
                    validatedToken.getRoles(),
                    validatedToken.getTokenType(),
                    validatedToken.getTokenId(),
                    validatedToken.getClientId(),
                    validatedToken.getAccountId(),
                    validatedToken.getIssuedAt()
            );
        } catch (ExpiredJwtException exception) {
            revokeExpiredSession(exception);
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        } catch (JwtException exception) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        } catch (IllegalArgumentException exception) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid session key in JWT");
        }
    }

    public void logout(TokenValidationRequest request, String authorizationHeader) {
        String token = extractToken(request, authorizationHeader);
        try {
            ValidatedToken validatedToken = jwtTokenValidator.validateToken(token);
            if (!StringUtils.hasText(validatedToken.getTokenId())) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT session key is required for logout");
            }
            sessionKeyService.revokeSession(UUID.fromString(validatedToken.getTokenId()));
        } catch (ExpiredJwtException exception) {
            revokeExpiredSession(exception);
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        } catch (JwtException exception) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        } catch (IllegalArgumentException exception) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid session key in JWT");
        }
    }

    private String extractToken(TokenValidationRequest request, String authorizationHeader) {
        if (request != null && StringUtils.hasText(request.getToken())) {
            return request.getToken().trim();
        }
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(JwtAuthenticationFilter.BEARER_PREFIX)) {
            return authorizationHeader.substring(JwtAuthenticationFilter.BEARER_PREFIX.length()).trim();
        }
        throw new AuthApiException(HttpStatus.BAD_REQUEST, "JWT token is required");
    }

    private void revokeExpiredSession(ExpiredJwtException exception) {
        if (exception.getClaims() == null || !StringUtils.hasText(exception.getClaims().getId())) {
            return;
        }
        try {
            sessionKeyService.revokeSession(UUID.fromString(exception.getClaims().getId()));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private AuthRegisterRequest normalizeRegisterRequest(AuthRegisterRequest request) {
        if (request == null) {
            return null;
        }

        AuthRegisterRequest normalized = new AuthRegisterRequest();
        normalized.setEmail(trimToNull(request.getEmail()));
        normalized.setDisplayName(trimToNull(request.getDisplayName()));
        normalized.setUsername(trimToNull(request.getUsername()));
        normalized.setPassword(request.getPassword());
        return normalized;
    }

    private AuthLoginRequest normalizeLoginRequest(AuthLoginRequest request) {
        if (request == null) {
            return null;
        }

        AuthLoginRequest normalized = new AuthLoginRequest();
        normalized.setUsername(trimToNull(request.getUsername()));
        normalized.setPassword(request.getPassword());
        return normalized;
    }

    private AdminLoginRequest normalizeAdminLoginRequest(AdminLoginRequest request) {
        if (request == null) {
            return null;
        }

        AdminLoginRequest normalized = new AdminLoginRequest();
        normalized.setEmail(trimToNull(request.getEmail()));
        normalized.setPassword(request.getPassword());
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recordLoginEvent(UUID clientId, String email, String outcome, String failureReason) {
        try {
            Map<String, Object> details = new HashMap<>();
            if (failureReason != null) {
                details.put("reason", failureReason);
            }
            String detailsJson = objectMapper.writeValueAsString(details);
            loginEventMapper.insertLoginEvent(clientId, email, outcome, detailsJson);
        } catch (Exception e) {
            System.err.println("Failed to record login event: " + e.getMessage());
        }
    }

}
