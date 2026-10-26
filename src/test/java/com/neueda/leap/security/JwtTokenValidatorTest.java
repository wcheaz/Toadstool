package com.neueda.leap.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("JWT Token Validator Unit Tests")
class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private JwtProperties jwtProperties;
    private KeyPair signingKeys;
    
    @Mock
    private JwtKeyProvider jwtKeyProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        signingKeys = generateKeyPair();
        jwtProperties = new JwtProperties();
        jwtProperties.setPublicKey(Base64.getEncoder().encodeToString(signingKeys.getPublic().getEncoded()));
        jwtProperties.setEnabled(true);
        
        when(jwtKeyProvider.getVerificationKey()).thenReturn(signingKeys.getPublic());
        
        validator = new JwtTokenValidator(jwtProperties, jwtKeyProvider);
    }

    @Test
    @DisplayName("validateToken accepts valid JWT with all required claims")
    void validateTokenAcceptsValid() {
        String token = generateValidToken();

        ValidatedToken result = validator.validateToken(token);

        assertTrue(result.isAuthenticated());
        assertNotNull(result.getSubject());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.getRoles().contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("validateToken rejects expired token")
    void validateTokenRejectsExpired() {
        String expiredToken = generateExpiredToken();

        assertThrows(Exception.class, () -> validator.validateToken(expiredToken));
    }

    @Test
    @DisplayName("validateToken rejects invalid token format")
    void validateTokenRejectsInvalidFormat() {
        String invalidToken = "not.a.valid.token.format";

        assertThrows(Exception.class, () -> validator.validateToken(invalidToken));
    }

    @Test
    @DisplayName("validateToken rejects token with invalid signature")
    void validateTokenRejectsInvalidSignature() {
        String tokenWithWrongSignature = generateTokenWithWrongSignature();

        assertThrows(Exception.class, () -> validator.validateToken(tokenWithWrongSignature));
    }

    @Test
    @DisplayName("validateToken rejects token missing issued-at claim")
    void validateTokenRejectsMissingIssuedAt() {
        String tokenMissingIssuedAt = Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_ADMIN")
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKeys.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        assertThrows(Exception.class, () -> validator.validateToken(tokenMissingIssuedAt));
    }

    @Test
    @DisplayName("validateToken returns unauthenticated token when JWT is disabled")
    void validateTokenReturnsUnauthenticatedWhenDisabled() {
        jwtProperties.setEnabled(false);
        String token = generateValidToken();

        ValidatedToken result = validator.validateToken(token);

        assertFalse(result.isAuthenticated());
        assertNull(result.getSubject());
    }

    /**
     * Generates a valid test JWT token with correct signature.
     */
    private String generateValidToken() {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_ADMIN,ROLE_USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKeys.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Generates an expired token.
     */
    private String generateExpiredToken() {
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_ADMIN")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(signingKeys.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Generates a token signed with a different secret to test signature validation.
     */
    private String generateTokenWithWrongSignature() {
        KeyPair wrongSigningKeys = generateKeyPair();
        return Jwts.builder()
                .setSubject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("roles", "ROLE_ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongSigningKeys.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA support is required for JWT tests", e);
        }
    }

}
