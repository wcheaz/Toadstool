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
import com.neueda.leap.security.JwtTokenService.IssuedTokenPair;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final ClientMapper clientMapper;
    private final AccountMapper accountMapper;
    private final AdminUserMapper adminUserMapper;
    private final AuthCredentialMapper authCredentialMapper;
    private final AdminCredentialMapper adminCredentialMapper;
    private final LoginEventMapper loginEventMapper;
    private final SessionKeyService sessionKeyService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenValidator jwtTokenValidator;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;


    public AuthService(ClientMapper clientMapper, AccountMapper accountMapper, AdminUserMapper adminUserMapper,
                       AuthCredentialMapper authCredentialMapper, AdminCredentialMapper adminCredentialMapper,
                       LoginEventMapper loginEventMapper,
                       SessionKeyService sessionKeyService, JwtTokenService jwtTokenService,
                       JwtTokenValidator jwtTokenValidator, PasswordEncoder passwordEncoder) {
        this.clientMapper = clientMapper;
        this.accountMapper = accountMapper;
        this.adminUserMapper = adminUserMapper;
        this.authCredentialMapper = authCredentialMapper;
        this.adminCredentialMapper = adminCredentialMapper;
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

        AdminCredential credential = adminCredentialMapper.selectByUsername(normalizedRequest.getEmail());
        if (credential == null || !passwordEncoder.matches(normalizedRequest.getPassword(), credential.getPasswordHash())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid admin email or password");
        }

        AdminUser adminUser = adminUserMapper.selectAdminUserById(credential.getAdminUserId());
        if (adminUser == null) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Invalid admin email or password");
        }

        if (adminUser.getRole() != AdminRole.ADMIN) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        if (adminUser.getStatus() != AdminUserStatus.ACTIVE) {
            throw new AuthApiException(HttpStatus.FORBIDDEN, "Admin user is not active");
        }

        UUID sessionKeyId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        IssuedTokenPair issuedTokens = jwtTokenService.issueTokenPair(
                sessionKeyId,
                adminUser.getAdminUserId(),
                null,
                null,
                adminUser.getEmail(),
                adminUser.getEmail(),
                "ADMIN"
        );

        if (issuedTokens.getAccessToken() == null || issuedTokens.getRefreshToken() == null) {
            throw new IllegalStateException("JWT authentication is disabled");
        }

        sessionKeyService.createAdminSession(
                sessionKeyId,
                credential.getCredentialId(),
                adminUser.getAdminUserId(),
                issuedAt,
                OffsetDateTime.ofInstant(issuedTokens.getAccessTokenExpiresAt(), ZoneOffset.UTC)
        );

        return new AuthResponse(
                issuedTokens.getAccessToken(),
                Duration.between(issuedAt.toInstant(), issuedTokens.getAccessTokenExpiresAt()).getSeconds(),
                issuedTokens.getRefreshToken(),
                Duration.between(issuedAt.toInstant(), issuedTokens.getRefreshTokenExpiresAt()).getSeconds(),
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
        IssuedTokenPair issuedTokens = jwtTokenService.issueTokenPair(
                sessionKeyId,
                client.getClientId(),
                client.getClientId(),
                account.getAccountId(),
                username,
                client.getEmail(),
                "CLIENT"
        );

        if (issuedTokens.getAccessToken() == null || issuedTokens.getRefreshToken() == null) {
            throw new IllegalStateException("JWT authentication is disabled");
        }

        sessionKeyService.createSession(
                sessionKeyId,
                credentialId,
                client.getClientId(),
                issuedAt,
                OffsetDateTime.ofInstant(issuedTokens.getAccessTokenExpiresAt(), ZoneOffset.UTC),
                hashToken(issuedTokens.getRefreshToken()),
                OffsetDateTime.ofInstant(issuedTokens.getRefreshTokenExpiresAt(), ZoneOffset.UTC)
        );

        return new AuthResponse(
                issuedTokens.getAccessToken(),
                Duration.between(issuedAt.toInstant(), issuedTokens.getAccessTokenExpiresAt()).getSeconds(),
                issuedTokens.getRefreshToken(),
                Duration.between(issuedAt.toInstant(), issuedTokens.getRefreshTokenExpiresAt()).getSeconds(),
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
        if (!isValidEmail(request.getEmail())) {
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

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email)
                && email.length() <= 254
                && EMAIL_PATTERN.matcher(email).matches();
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

    public AuthResponse refresh(TokenValidationRequest request, String authorizationHeader) {
        String token = extractToken(request, authorizationHeader);
        try {
            ValidatedToken validatedToken = jwtTokenValidator.validateToken(token);
            if (!validatedToken.isAuthenticated()) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT authentication is disabled");
            }
            if (!"REFRESH".equalsIgnoreCase(validatedToken.getTokenType())) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Only refresh tokens can be used to refresh the session");
            }

            if (!StringUtils.hasText(validatedToken.getSubject())) {
                throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Refresh token subject is required");
            }

            String refreshTokenHash = hashToken(token);
            IssuedTokenPair issuedTokens;

            if ("ADMIN".equalsIgnoreCase(stripRolePrefix(validatedToken.getRoles()))) {
                AdminUser adminUser = adminUserMapper.selectActiveAdminUserByEmail(validatedToken.getEmail());
                if (adminUser == null) {
                    throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Admin account no longer exists");
                }

                issuedTokens = jwtTokenService.issueTokenPair(
                        null,
                        UUID.fromString(validatedToken.getSubject()),
                        null,
                        null,
                        adminUser.getEmail(),
                        adminUser.getEmail(),
                        "ADMIN"
                );
            } else {
                if (!StringUtils.hasText(validatedToken.getClientId()) || !StringUtils.hasText(validatedToken.getAccountId())) {
                    throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Client refresh token is missing required claims");
                }

                AuthSession session = sessionKeyService.getActiveSessionByRefreshTokenHash(refreshTokenHash);
                if (session == null || session.getSessionKeyId() == null) {
                    throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT session is no longer active");
                }

                if (!validatedToken.getSubject().equals(session.getClientId().toString())) {
                    throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Refresh token subject does not match the active session");
                }

                issuedTokens = jwtTokenService.issueTokenPair(
                        session.getSessionKeyId(),
                        UUID.fromString(validatedToken.getSubject()),
                        UUID.fromString(validatedToken.getClientId()),
                        UUID.fromString(validatedToken.getAccountId()),
                        validatedToken.getUsername(),
                        validatedToken.getEmail(),
                        stripRolePrefix(validatedToken.getRoles())
                );

                if (issuedTokens.getAccessToken() == null || issuedTokens.getRefreshToken() == null) {
                    throw new IllegalStateException("JWT authentication is disabled");
                }

                boolean sessionRotated = sessionKeyService.rotateSessionTokens(
                        session.getSessionKeyId(),
                        OffsetDateTime.ofInstant(issuedTokens.getAccessTokenExpiresAt(), ZoneOffset.UTC),
                        hashToken(issuedTokens.getRefreshToken()),
                        OffsetDateTime.ofInstant(issuedTokens.getRefreshTokenExpiresAt(), ZoneOffset.UTC)
                );
                if (!sessionRotated) {
                    throw new AuthApiException(HttpStatus.UNAUTHORIZED, "JWT session is no longer active");
                }
                return buildClientAuthResponse(validatedToken, issuedTokens);
            }

            if (issuedTokens.getAccessToken() == null || issuedTokens.getRefreshToken() == null) {
                throw new IllegalStateException("JWT authentication is disabled");
            }

            return buildAdminAuthResponse(validatedToken, issuedTokens);
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

    private AuthResponse buildClientAuthResponse(ValidatedToken validatedToken, IssuedTokenPair issuedTokens) {
        if (!StringUtils.hasText(validatedToken.getClientId()) || !StringUtils.hasText(validatedToken.getAccountId())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Client session is missing required claims");
        }

        Client client = clientMapper.selectClientById(UUID.fromString(validatedToken.getClientId()));
        if (client == null) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Client account no longer exists");
        }

        List<Account> accounts = accountMapper.selectAccountsByClientId(UUID.fromString(validatedToken.getClientId()), 1, 0);
        if (accounts.isEmpty()) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Client account no longer exists");
        }

        return new AuthResponse(
                issuedTokens.getAccessToken(),
                Duration.between(Instant.now(), issuedTokens.getAccessTokenExpiresAt()).getSeconds(),
                issuedTokens.getRefreshToken(),
                Duration.between(Instant.now(), issuedTokens.getRefreshTokenExpiresAt()).getSeconds(),
                client.getClientId(),
                UUID.fromString(validatedToken.getAccountId()),
                client.getDisplayName(),
                validatedToken.getUsername(),
                validatedToken.getEmail(),
                roleForResponse(validatedToken.getRoles())
        );
    }

    private AuthResponse buildAdminAuthResponse(ValidatedToken validatedToken, IssuedTokenPair issuedTokens) {
        if (!StringUtils.hasText(validatedToken.getEmail())) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Admin session is missing an email claim");
        }

        AdminUser adminUser = adminUserMapper.selectActiveAdminUserByEmail(validatedToken.getEmail());
        if (adminUser == null) {
            throw new AuthApiException(HttpStatus.UNAUTHORIZED, "Admin account no longer exists");
        }

        return new AuthResponse(
                issuedTokens.getAccessToken(),
                Duration.between(Instant.now(), issuedTokens.getAccessTokenExpiresAt()).getSeconds(),
                issuedTokens.getRefreshToken(),
                Duration.between(Instant.now(), issuedTokens.getRefreshTokenExpiresAt()).getSeconds(),
                null,
                null,
                adminUser.getDisplayName(),
                validatedToken.getUsername(),
                validatedToken.getEmail(),
                roleForResponse(validatedToken.getRoles())
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 support is required", e);
        }
    }

    private String stripRolePrefix(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        if (role.startsWith("ROLE_")) {
            return role.substring("ROLE_".length());
        }
        return role;
    }

    private String roleForResponse(String role) {
        return stripRolePrefix(role);
    }

    private AuthRegisterRequest normalizeRegisterRequest(AuthRegisterRequest request) {
        if (request == null) {
            return null;
        }

        AuthRegisterRequest normalized = new AuthRegisterRequest();
        normalized.setEmail(trimToNull(request.getEmail()));
        normalized.setDisplayName(trimToNull(request.getDisplayName()));
        normalized.setUsername(normalizeUsername(request.getUsername()));
        normalized.setPassword(request.getPassword());
        return normalized;
    }

    private AuthLoginRequest normalizeLoginRequest(AuthLoginRequest request) {
        if (request == null) {
            return null;
        }

        AuthLoginRequest normalized = new AuthLoginRequest();
        normalized.setUsername(normalizeUsername(request.getUsername()));
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

    private String normalizeUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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
