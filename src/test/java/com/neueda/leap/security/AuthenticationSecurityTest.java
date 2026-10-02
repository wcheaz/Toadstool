package com.neueda.leap.security;

import com.neueda.leap.ToadstoolApplication;
import com.neueda.leap.auth.AuthResponse;
import com.neueda.leap.auth.TokenValidationResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ToadstoolApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Authentication Security Tests")
class AuthenticationSecurityTest {

    private static final KeyPair TOKEN_SIGNING_KEYS = generateKeyPair();
    private static final String PLATFORM_ACTIVITY_PATH = "/api/admin/analytics/platform-activity";
    private static final String ADMIN_JDBC_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String TEST_DB_NAME = "toadstool_auth_" + UUID.randomUUID().toString().replace("-", "");
    private static final String TEST_JDBC_URL = "jdbc:postgresql://localhost:5432/" + TEST_DB_NAME;
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "n3u3d4!";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @DynamicPropertySource
    static void registerJwtProperties(DynamicPropertyRegistry registry) {
        recreateTestDatabase();
        registry.add("auth.jwt.enabled", () -> true);
        registry.add("auth.jwt.public-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPublic().getEncoded()));
        registry.add("auth.jwt.private-key",
                () -> Base64.getEncoder().encodeToString(TOKEN_SIGNING_KEYS.getPrivate().getEncoded()));
        registry.add("spring.datasource.url", () -> TEST_JDBC_URL);
        registry.add("spring.datasource.username", () -> DB_USERNAME);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
    }

    @AfterAll
    static void cleanUpTestDatabase() {
        try (Connection connection = DriverManager.getConnection(ADMIN_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + TEST_DB_NAME
                    + "' AND pid <> pg_backend_pid()");
            statement.execute("DROP DATABASE IF EXISTS " + TEST_DB_NAME);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to drop temporary authentication test database " + TEST_DB_NAME, e);
        }
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

    private static void recreateTestDatabase() {
        try (Connection connection = DriverManager.getConnection(ADMIN_JDBC_URL, DB_USERNAME, DB_PASSWORD);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute("DROP DATABASE IF EXISTS " + TEST_DB_NAME);
            statement.execute("CREATE DATABASE " + TEST_DB_NAME);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create temporary authentication test database " + TEST_DB_NAME, e);
        }
    }

    private record RegisteredUser(String username, String email, AuthResponse authResponse) {
    }
}
