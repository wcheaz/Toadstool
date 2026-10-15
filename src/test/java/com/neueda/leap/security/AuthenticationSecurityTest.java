package com.neueda.leap.security;

import com.neueda.leap.ToadstoolApplication;
import com.neueda.leap.auth.AuthResponse;
import com.neueda.leap.auth.TokenValidationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import org.springframework.test.context.TestPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ToadstoolApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/toadstool_db",
    "spring.datasource.username=toadstool_user",
    "spring.datasource.password=toadstool_password",
    "spring.flyway.url=jdbc:postgresql://localhost:5432/toadstool_db",
    "spring.flyway.user=toadstool_user",
    "spring.flyway.password=toadstool_password"
})
@DisplayName("Authentication Security Tests")
class AuthenticationSecurityTest {

    private static final KeyPair TOKEN_SIGNING_KEYS = generateKeyPair();
    private static final String PLATFORM_ACTIVITY_PATH = "/api/admin/analytics/platform-activity";
    private static final String ADMIN_PROBE_PATH = "/api/admin/probe";
    private static final String SEEDED_ADMIN_EMAIL = "david.admin@neueda.com";
    private static final String SEEDED_ADMIN_PASSWORD = "Admin123!";
    private static final String TEST_DB_HOST = config("TEST_DB_HOST", "localhost");
    private static final String TEST_DB_PORT = config("TEST_DB_PORT", "5432");
    private static final String TEST_DB_NAME = config("TEST_DB_NAME", "toadstool_db");
    private static final String TEST_DB_USERNAME = config("TEST_DB_USERNAME", "toadstool_user");
    private static final String TEST_DB_PASSWORD = config("TEST_DB_PASSWORD", "toadstool_password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEEDED_CLIENT_EMAIL = "alice.johnson@example.com";

    @DynamicPropertySource
    static void registerJwtProperties(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.enabled", () -> true);
        registry.add("auth.jwt.public-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPublic().getEncoded()));
        registry.add("auth.jwt.private-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPrivate().getEncoded()));
        registry.add("spring.datasource.url",
            () -> "jdbc:postgresql://" + TEST_DB_HOST + ":" + TEST_DB_PORT + "/" + TEST_DB_NAME);
        registry.add("spring.datasource.username", () -> TEST_DB_USERNAME);
        registry.add("spring.datasource.password", () -> TEST_DB_PASSWORD);
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
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");

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
    @DisplayName("Registered users can log in again and still receive valid JWTs")
    void loginIssuesJwtForClientCredential() {
        RegisteredUser registeredUser = registerUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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
        assertEquals(registeredUser.authResponse().getClientId(), loginResponse.getBody().getClientId());
        assertEquals(registeredUser.authResponse().getAccountId(), loginResponse.getBody().getAccountId());
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
    assertEquals("Alice Johnson", client.path("displayName").asText());
    assertTrue(client.path("accountCount").asInt() >= 1);
    String clientId = client.path("clientId").asText();

    int accessBefore = getJson("/api/admin/audit/access-events?clientId=" + clientId, token)
        .path("pagination").path("totalCount").asInt();

    JsonNode detail = getJson("/api/admin/clients/" + clientId, token);
    assertEquals(clientId, detail.path("client").path("clientId").asText());
    assertTrue(detail.path("accounts").size() >= 1);

    JsonNode access = getJson("/api/admin/audit/access-events?clientId=" + clientId, token);
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
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "admin@example.com")
                .claim("roles", "ROLE_ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
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

    private static String config(String key, String defaultValue) {
        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private record RegisteredUser(String username, String email, AuthResponse authResponse) {
    }
}
