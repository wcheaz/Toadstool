package com.neueda.leap.security;

import com.neueda.leap.ToadstoolApplication;
import com.neueda.leap.auth.AuthResponse;
import com.neueda.leap.auth.TokenValidationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ToadstoolApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Authentication Security Tests")
class AuthenticationSecurityTest {

    private static final KeyPair TOKEN_SIGNING_KEYS = generateKeyPair();
    private static final String PLATFORM_ACTIVITY_PATH = "/api/admin/analytics/platform-activity";
    private static final String ADMIN_PROBE_PATH = "/api/admin/probe";
    private static final String SEEDED_ADMIN_EMAIL = "david.admin@neueda.com";
    private static final String SEEDED_ADMIN_PASSWORD = "Admin123!";
    private static final String ADMIN_JDBC_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String TEST_DB_NAME = "toadstool_test";
    private static final String TEST_JDBC_URL = "jdbc:postgresql://localhost:5432/" + TEST_DB_NAME;
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "n3u3d4!";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEEDED_CLIENT_EMAIL = "alice.johnson@example.com";

    @Autowired
    private com.neueda.leap.auth.SessionKeyService sessionKeyService;

    @DynamicPropertySource
    static void registerJwtProperties(DynamicPropertyRegistry registry) {
        recreateTestDatabase();
        registry.add("auth.jwt.enabled", () -> true);
        registry.add("auth.jwt.access-token-ttl-minutes", () -> 1);
        registry.add("auth.jwt.public-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPublic().getEncoded()));
        registry.add("auth.jwt.private-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPrivate().getEncoded()));
        registry.add("spring.datasource.url",
                () -> TEST_JDBC_URL);
        registry.add("spring.datasource.username", () -> DB_USERNAME);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.flyway.url", () -> TEST_JDBC_URL);
        registry.add("spring.flyway.user", () -> DB_USERNAME);
        registry.add("spring.flyway.password", () -> DB_PASSWORD);
    }

    @Test
    @DisplayName("GET /api/health remains public without authentication")
    void healthEndpointAllowsAnonymousAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/health"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity rejects unauthenticated requests")
    void protectedEndpointRejectsAnonymousAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(url(PLATFORM_ACTIVITY_PATH), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/holdings rejects unauthenticated requests")
    void holdingsEndpointRejectsAnonymousAccess() {
        String accountId = UUID.randomUUID().toString();

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/accounts/" + accountId + "/holdings"),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity rejects invalid JWTs")
    void protectedEndpointRejectsInvalidJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                url(PLATFORM_ACTIVITY_PATH),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity accepts valid JWTs signed by the trusted issuer")
    void protectedEndpointAllowsValidJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + generateValidToken());

        ResponseEntity<String> response = restTemplate.exchange(
                url(PLATFORM_ACTIVITY_PATH),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Registered users receive JWTs whose subject is their client ID")
    void registerIssuesJwtWithClientIdSubject() {
        RegisteredUser registeredUser = registerUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<TokenValidationResponse> validation = restTemplate.exchange(
                url("/api/auth/validate"),
                HttpMethod.POST,
                new HttpEntity<>("{\"token\":\"" + registeredUser.authResponse().getAccessToken() + "\"}", headers),
                TokenValidationResponse.class
        );

        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertNotNull(validation.getBody());
        assertEquals(registeredUser.authResponse().getClientId().toString(), validation.getBody().getSubject());
        assertEquals(registeredUser.username(), validation.getBody().getUsername());
        assertEquals(registeredUser.authResponse().getAccountId().toString(), validation.getBody().getAccountId());
    }

    @Test
    @DisplayName("A token remains valid while auth_sessions.revoked_at is null")
    void tokenIsValidWhenRevokedAtIsNull() {
        RegisteredUser registeredUser = registerUser();
        UUID sessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());

        assertEquals(null, readRevokedAt(sessionKeyId));

        ResponseEntity<TokenValidationResponse> validation = restTemplate.exchange(
                url("/api/auth/validate"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                TokenValidationResponse.class
        );

        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertNotNull(validation.getBody());
        assertEquals(sessionKeyId.toString(), validation.getBody().getSessionKeyId());
    }

    @Test
    @DisplayName("Expired auth sessions are revoked by the cleanup logic")
    void expiredAuthSessionIsRevokedByCleanup() {
        RegisteredUser registeredUser = registerUser();
        UUID sessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());
        markSessionExpired(sessionKeyId);

        int revokedCount = sessionKeyService.revokeExpiredSessions();

        assertTrue(revokedCount > 0);
        assertNotNull(readRevokedAt(sessionKeyId));
    }

    @Test
    @DisplayName("Registered users can log in again without revoking the previous session")
    void loginIssuesJwtForClientCredential() {
        RegisteredUser registeredUser = registerUser();
        UUID clientId = registeredUser.authResponse().getClientId();
        long initialSessionCount = countSessionsByClientId(clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        UUID originalSessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());

        ResponseEntity<AuthResponse> loginResponse = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"username\":\"" + registeredUser.username()
                                + "\",\"password\":\"Password123!\"}",
                        headers
                ),
                AuthResponse.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getAccessToken());
        assertEquals(clientId, loginResponse.getBody().getClientId());
        assertEquals(registeredUser.authResponse().getAccountId(), loginResponse.getBody().getAccountId());
        assertNotEquals(registeredUser.authResponse().getAccessToken(), loginResponse.getBody().getAccessToken());
        assertEquals(null, readRevokedAt(originalSessionKeyId));
        assertNotEquals(originalSessionKeyId, sessionKeyId(loginResponse.getBody().getAccessToken()));
        assertEquals(initialSessionCount + 1, countSessionsByClientId(clientId));
    }

    @Test
    @DisplayName("A client token can access only its own client data and not another client's")
    void clientTokenCanAccessOnlyOwnedData() {
        RegisteredUser firstUser = registerUser();
        RegisteredUser secondUser = registerUser();

        ResponseEntity<String> ownResponse = restTemplate.exchange(
                url("/api/clients/" + firstUser.authResponse().getClientId()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(firstUser.authResponse().getAccessToken())),
                String.class
        );

        ResponseEntity<String> forbiddenResponse = restTemplate.exchange(
                url("/api/clients/" + secondUser.authResponse().getClientId()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(firstUser.authResponse().getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.OK, ownResponse.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());
    }

    @Test
    @DisplayName("Logging out revokes the server-side session for the access token")
    void logoutRevokesSession() {
        RegisteredUser registeredUser = registerUser();
        UUID sessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                Void.class
        );

        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                url("/api/clients/" + registeredUser.authResponse().getClientId()),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, protectedResponse.getStatusCode());
        assertNotNull(readRevokedAt(sessionKeyId));
    }

    @Test
    @DisplayName("Repeated revocation keeps the auth_sessions.revoked_at timestamp stable")
    void repeatedLogoutKeepsRevokedAtStable() throws InterruptedException {
        RegisteredUser registeredUser = registerUser();
        UUID sessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());

        ResponseEntity<Void> firstLogout = restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                Void.class
        );

        OffsetDateTime firstRevokedAt = readRevokedAt(sessionKeyId);
        Thread.sleep(1200L);

        ResponseEntity<Void> secondLogout = restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                Void.class
        );

        OffsetDateTime secondRevokedAt = readRevokedAt(sessionKeyId);

        assertEquals(HttpStatus.NO_CONTENT, firstLogout.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, secondLogout.getStatusCode());
        assertNotNull(firstRevokedAt);
        assertNotNull(secondRevokedAt);
        assertEquals(firstRevokedAt, secondRevokedAt);
    }

    @Test
    @DisplayName("Expired access tokens are revoked when the session is checked")
    void expiredSessionIsRevokedWhenValidated() {
        RegisteredUser registeredUser = registerUser();
        UUID sessionKeyId = sessionKeyId(registeredUser.authResponse().getAccessToken());
        markSessionExpired(sessionKeyId);

        ResponseEntity<TokenValidationResponse> validation = restTemplate.exchange(
                url("/api/auth/validate"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                TokenValidationResponse.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, validation.getStatusCode());
        assertNotNull(readRevokedAt(sessionKeyId));
    }

    @Test
    @DisplayName("A new sign in issues a fresh access token after the previous session is revoked")
    void reloginIssuesFreshAccessTokenAfterRevocation() {
        RegisteredUser registeredUser = registerUser();
        String originalAccessToken = registeredUser.authResponse().getAccessToken();
        UUID clientId = registeredUser.authResponse().getClientId();
        long sessionCountAfterRegister = countSessionsByClientId(clientId);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(originalAccessToken)),
                Void.class
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> reloginResponse = restTemplate.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"username\":\"" + registeredUser.username()
                                + "\",\"password\":\"Password123!\"}",
                        headers
                ),
                AuthResponse.class
        );

        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());
        assertNotNull(reloginResponse.getBody());
        assertNotNull(reloginResponse.getBody().getAccessToken());
        assertNotEquals(originalAccessToken, reloginResponse.getBody().getAccessToken());
        assertEquals(sessionCountAfterRegister + 1, countSessionsByClientId(clientId));
    }

    @Test
    @DisplayName("Expired JWTs are rejected by protected endpoints")
    void expiredJwtIsRejectedByProtectedEndpoint() throws InterruptedException {
        String expiredToken = generateTokenWithLifetime(Duration.ofSeconds(1));
        Thread.sleep(1500L);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken);

        ResponseEntity<String> response = restTemplate.exchange(
                url(PLATFORM_ACTIVITY_PATH),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Refreshing a client session issues a refresh token with a 15-minute TTL")
    void refreshIssuesRefreshTokenWithFifteenMinuteTtl() {
        RegisteredUser registeredUser = registerUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.exchange(
                url("/api/auth/refresh"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"token\":\"" + registeredUser.authResponse().getRefreshToken() + "\"}",
                        headers
                ),
                AuthResponse.class
        );

        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody());
        assertEquals(Duration.ofMinutes(15).getSeconds(), jwtLifetimeSeconds(refreshResponse.getBody().getRefreshToken()));
    }

    @Test
    @DisplayName("Client JWTs cannot access admin-only analytics endpoints")
    void clientJwtCannotAccessAdminEndpoint() {
        RegisteredUser registeredUser = registerUser();

        ResponseEntity<String> response = restTemplate.exchange(
                url(PLATFORM_ACTIVITY_PATH),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Client JWTs cannot access admin probe endpoint")
    void clientJwtCannotAccessAdminProbeEndpoint() {
        RegisteredUser registeredUser = registerUser();

        ResponseEntity<String> response = restTemplate.exchange(
                url(ADMIN_PROBE_PATH),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("Admin login can access admin probe endpoint")
    void adminLoginCanAccessAdminProbeEndpoint() {
        AuthResponse adminAuth = adminLogin();
        assertEquals("ADMIN", adminAuth.getRole());

        ResponseEntity<String> probeResponse = restTemplate.exchange(
                url(ADMIN_PROBE_PATH),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminAuth.getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.OK, probeResponse.getStatusCode());
    }

    @Test
    @DisplayName("Admin logout revokes the admin session for the access token")
    void adminLogoutRevokesSession() {
        AuthResponse adminAuth = adminLogin();

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(adminAuth.getAccessToken())),
                Void.class
        );

        ResponseEntity<String> probeResponse = restTemplate.exchange(
                url(ADMIN_PROBE_PATH),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminAuth.getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.NO_CONTENT, logoutResponse.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, probeResponse.getStatusCode());
    }

    @Test
    @DisplayName("Admin credentials cannot be written without the provisioning flag")
    void adminCredentialsAreReadOnly() {
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "INSERT INTO trading.admin_credentials (admin_user_id, username, password_hash) "
                        + "SELECT admin_user_id, 'intruder@example.com', 'x' FROM trading.admin_users LIMIT 1"
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "UPDATE trading.admin_credentials SET password_hash = 'x' WHERE username = ?", SEEDED_ADMIN_EMAIL
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
                "DELETE FROM trading.admin_credentials WHERE username = ?", SEEDED_ADMIN_EMAIL
        ));
    }

    @Test
    @DisplayName("Admin can search clients and the client view is recorded in the access log")
    void adminClientSearchAndViewIsAudited() throws Exception {
        String token = adminLogin().getAccessToken();

        JsonNode search = getJson("/api/admin/clients?q=" + SEEDED_CLIENT_EMAIL, token);
        assertEquals(1, search.path("pagination").path("totalCount").asInt());
        JsonNode client = search.path("items").get(0);
        assertEquals(SEEDED_CLIENT_EMAIL, client.path("displayName").asText());
        assertTrue(client.path("accountCount").asInt() >= 1);
        String clientId = client.path("clientId").asText();

        int accessBefore = getJson("/api/admin/audit/access-events?clientId=" + clientId, token)
                .path("pagination").path("totalCount").asInt();

        JsonNode detail = getJson("/api/admin/clients/" + clientId, token);
        assertEquals(clientId, detail.path("client").path("clientId").asText());
        assertTrue(detail.path("accounts").size() >= 1);

        JsonNode access = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            access = getJson("/api/admin/audit/access-events?clientId=" + clientId, token);
            if (access.path("pagination").path("totalCount").asInt() >= accessBefore + 1) {
                break;
            }
            Thread.sleep(150);
        }

        assertNotNull(access);
        assertEquals(accessBefore + 1, access.path("pagination").path("totalCount").asInt());
        assertEquals("VIEW_CLIENT", access.path("items").get(0).path("action").asText());
        assertEquals(SEEDED_ADMIN_EMAIL, access.path("items").get(0).path("adminEmail").asText());
    }

    @Test
    @DisplayName("Admin sees real holdings, fills and order status for a client account")
    void adminSeesRealAccountData() throws Exception {
        String token = adminLogin().getAccessToken();
        String clientId = getJson("/api/admin/clients?q=" + SEEDED_CLIENT_EMAIL, token)
                .path("items").get(0).path("clientId").asText();
        String accountId = getJson("/api/admin/clients/" + clientId, token)
                .path("accounts").get(0).path("accountId").asText();

        JsonNode holdings = getJson("/api/accounts/" + accountId + "/holdings", token);
        assertTrue(holdings.path("holdings").size() > 0);
        assertTrue(holdings.path("holdings").get(0).hasNonNull("symbol"));

        JsonNode fills = getJson("/api/accounts/" + accountId + "/fills?limit=50", token);
        assertTrue(fills.path("items").size() > 0);
        assertTrue(fills.path("items").get(0).has("fee"));

        JsonNode orders = getJson("/api/accounts/" + accountId + "/orders?limit=50", token);
        assertEquals("FILLED", orders.path("items").get(0).path("status").asText());

        JsonNode cashEvents = getJson("/api/accounts/" + accountId + "/cash-events", token);
        assertTrue(cashEvents.path("pagination").has("totalCount"));
    }

    @Test
    @DisplayName("Admin audit and analytics endpoints return live data")
    void adminAuditAndAnalyticsReturnLiveData() throws Exception {
        String token = adminLogin().getAccessToken();

        JsonNode logins = getJson("/api/admin/audit/login-events?outcome=SUCCESS&limit=5", token);
        assertTrue(logins.path("pagination").path("totalCount").asInt() > 0);
        logins.path("items").forEach(item -> assertEquals("SUCCESS", item.path("outcome").asText()));

        JsonNode trades = getJson("/api/admin/audit/trade-events?limit=5", token);
        assertTrue(trades.path("pagination").path("totalCount").asInt() > 0);

        JsonNode platform = getJson("/api/admin/analytics/platform-activity?limit=5", token);
        assertTrue(platform.path("pagination").path("totalCount").asInt() > 0);

        JsonNode insights = getJson("/api/admin/analytics/insights", token);
        assertTrue(insights.path("overview").path("totalClients").asInt() > 0);
        assertTrue(insights.path("insights").size() > 0);
    }

    @Test
    @DisplayName("Client JWTs cannot use the admin client oversight endpoints")
    void clientJwtCannotSearchClients() {
        RegisteredUser registeredUser = registerUser();

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/admin/clients"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(registeredUser.authResponse().getAccessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private JsonNode getJson(String path, String accessToken) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(accessToken)),
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode(), "GET " + path);
        return objectMapper.readTree(response.getBody());
    }

    private AuthResponse adminLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.exchange(
                url("/api/auth/admin/login"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"email\":\"" + SEEDED_ADMIN_EMAIL
                                + "\",\"password\":\"" + SEEDED_ADMIN_PASSWORD + "\"}",
                        headers
                ),
                AuthResponse.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getAccessToken());
        return loginResponse.getBody();
    }

    private String generateValidToken() {
        return generateTokenWithLifetime(Duration.ofHours(1));
    }

    private String generateTokenWithLifetime(Duration lifetime) {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "admin@example.com")
                .claim("roles", "ROLE_ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + lifetime.toMillis()))
                .signWith(TOKEN_SIGNING_KEYS.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA support is required for JWT security tests", e);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }

    private UUID sessionKeyId(String accessToken) {
        return UUID.fromString(Jwts.parserBuilder()
                .setSigningKey(TOKEN_SIGNING_KEYS.getPublic())
                .build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getId());
    }

    private long jwtLifetimeSeconds(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(TOKEN_SIGNING_KEYS.getPublic())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()).getSeconds();
    }

    private OffsetDateTime readRevokedAt(UUID sessionKeyId) {
        try (Connection connection = DriverManager.getConnection(TEST_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT revoked_at FROM trading.auth_sessions WHERE session_key_id = ?")) {
            statement.setObject(1, sessionKeyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Session not found: " + sessionKeyId);
                }
                return resultSet.getObject(1, OffsetDateTime.class);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read revoked_at for session " + sessionKeyId, e);
        }
    }

    private long countSessionsByClientId(UUID clientId) {
        try (Connection connection = DriverManager.getConnection(TEST_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM trading.auth_sessions WHERE client_id = ?")) {
            statement.setObject(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Failed to count sessions for client " + clientId);
                }
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count sessions for client " + clientId, e);
        }
    }

    private void markSessionExpired(UUID sessionKeyId) {
        try (Connection connection = DriverManager.getConnection(TEST_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE trading.auth_sessions SET access_token_expires_at = ? WHERE session_key_id = ?")) {
            statement.setObject(1, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            statement.setObject(2, sessionKeyId);
            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("Expected to update one session, updated " + updated);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to expire session " + sessionKeyId, e);
        }
    }

    private static void recreateTestDatabase() {
        try (Connection connection = DriverManager.getConnection(ADMIN_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + TEST_DB_NAME
                    + "' AND pid <> pg_backend_pid()");
            statement.execute("DROP DATABASE IF EXISTS " + TEST_DB_NAME);
            statement.execute("CREATE DATABASE " + TEST_DB_NAME);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create temporary authentication test database " + TEST_DB_NAME, e);
        }
    }

    private RegisteredUser registerUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "client_" + suffix;
        String email = "client_" + suffix + "@example.com";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(
                url("/api/auth/register"),
                HttpMethod.POST,
                new HttpEntity<>(
                        "{\"email\":\"" + email
                                + "\",\"displayName\":\"Client " + suffix
                                + "\",\"username\":\"" + username
                                + "\",\"password\":\"Password123!\"}",
                        headers
                ),
                AuthResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getAccessToken());
        return new RegisteredUser(username, email, response.getBody());
    }

    private record RegisteredUser(String username, String email, AuthResponse authResponse) {
    }
}
