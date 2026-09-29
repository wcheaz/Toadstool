# Authentication and JWT Overview

This document is the single source of truth for the JWT authentication work added to Toadstool: what was added, how it is configured, how request authentication works, which endpoints are protected, and how it was verified.

## Goal

The application now issues and validates JWTs locally from username/password login **without calling an identity service at request time**.

The trust model is:

- this Spring Boot service signs tokens with its private key
- this Spring Boot service is configured with the matching public key
- incoming bearer tokens are verified locally
- protected endpoints reject requests that are missing a valid token

## What Was Added

### Dependencies

Added in [pom.xml](C:/Users/Administrator/Toadstool/pom.xml):

- `spring-boot-starter-security`
- `jjwt-api`
- `jjwt-impl`
- `jjwt-jackson`
- `spring-security-test`

### Runtime Security Classes

- [JwtProperties.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtProperties.java)  
  Binds `auth.jwt.*` configuration.

- [JwtTokenValidator.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtTokenValidator.java)  
  Validates JWT signatures and required claims using a configured public key.

- [ValidatedToken.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/ValidatedToken.java)  
  Holds the validated token data used to build Spring Security authentication.

- [JwtAuthenticationFilter.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtAuthenticationFilter.java)  
  Reads the `Authorization` header, validates bearer tokens, and populates the `SecurityContext`.

- [SecurityConfig.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/SecurityConfig.java)  
  Registers the stateless security filter chain, allows public endpoints, and requires authentication everywhere else.

- [AuthController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthController.java)  
  Exposes register and login endpoints.

- [AuthService.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthService.java)  
  Creates accounts, hashes passwords, and mints JWTs.

- [AuthCredentialMapper.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthCredentialMapper.java) and [AuthSessionMapper.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthSessionMapper.java)  
  Persist usernames/password hashes and session rows.

### Tests Added or Updated

- [JwtTokenValidatorTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/security/JwtTokenValidatorTest.java)  
  Unit tests for signature validation, expiry, invalid format, invalid signature, missing `iat`, and disabled-auth behavior.

- [AuthenticationSecurityTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/security/AuthenticationSecurityTest.java)  
  Real integration test that boots the actual application over HTTP, provisions a temporary PostgreSQL database, and verifies endpoint behavior with anonymous, invalid-token, and valid-token requests.

- [FlywayTestExecutionListener.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/FlywayTestExecutionListener.java)  
  Adjusted so non-database Spring test slices are not broken by unconditional Flyway setup.

## Configuration

Configured in [application.yml](C:/Users/Administrator/Toadstool/src/main/resources/application.yml) and [application-test.yml](C:/Users/Administrator/Toadstool/src/main/resources/application-test.yml):

```yaml
auth:
  jwt:
    public-key: <base64-encoded-x509-public-key>
    private-key: <base64-encoded-pkcs8-private-key>
    issuer: toadstool
    access-token-ttl-minutes: 60
    refresh-token-ttl-days: 30
    enabled: true
```

### Meaning

- `auth.jwt.public-key`  
  Base64-encoded X.509 public key.

- `auth.jwt.private-key`  
  Base64-encoded PKCS#8 private key used to sign JWTs.

- `auth.jwt.issuer`  
  Issuer written into minted tokens.

- `auth.jwt.access-token-ttl-minutes`  
  Lifetime for access tokens.

- `auth.jwt.refresh-token-ttl-days`  
  Lifetime for refresh tokens.

- `auth.jwt.enabled`  
  Enables or disables JWT enforcement.

If JWT is enabled and either key is missing or malformed, startup fails fast.

## How Authentication Works

### 1. Request arrives

Client calls the API with:

```http
Authorization: Bearer <jwt>
```

### 2. Filter extracts the token

[JwtAuthenticationFilter.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtAuthenticationFilter.java) runs once per request and:

- reads the `Authorization` header
- checks for the `Bearer ` prefix
- extracts the token string

### 3. Token is validated locally

[JwtTokenValidator.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtTokenValidator.java):

- decodes the configured Base64 public key
- builds an RSA or EC `PublicKey`
- verifies the JWT signature using JJWT
- enforces expiration
- requires:
  - `sub`
  - `iat`
- extracts:
  - `sub`
  - `email`
  - `roles`

No request is made to the identity provider during validation.

### 4. Spring Security authentication is created

If validation succeeds:

- a [ValidatedToken.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/ValidatedToken.java) is created
- the filter converts comma-separated `roles` into `SimpleGrantedAuthority` values
- a `UsernamePasswordAuthenticationToken` is stored in the `SecurityContext`

### 5. Authorization rules are applied

[SecurityConfig.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/SecurityConfig.java) applies these rules:

- `/api/health` and `/api/health/**` -> public
- `/api/auth/**` -> public
- every other request -> authenticated

### 6. Failures return 401

If there is no authenticated principal for a protected endpoint, the configured authentication entry point returns:

```json
{
  "error": "Unauthorized",
  "message": "<spring security message>"
}
```

## Security Model

### Stateless

The API is configured as stateless:

- no server session is used for auth
- no login cookie is required
- each protected request must carry its own bearer token

### Offline Verification

This service **trusts tokens by cryptographic verification**, not by introspection:

- no direct identity-service call
- no token lookup round-trip
- signature verification happens locally

### Algorithms Supported by Current Validator

The current validator accepts public keys that decode as:

- RSA X.509 public keys
- EC X.509 public keys

The current tests use **RS256**.

## Protected Endpoint Behavior

Current behavior:

- public health endpoint is accessible anonymously
- protected admin/API endpoints reject:
  - no token
  - malformed token
  - invalid signature
  - expired token
- protected endpoints accept a valid signed token

Because [SecurityConfig.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/SecurityConfig.java) ends with `anyRequest().authenticated()`, protection applies broadly across the API, not just a few named routes.

## Example Token Shape

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": "ROLE_ADMIN,ROLE_USER",
  "iat": 1695081600,
  "exp": 1695085200
}
```

Notes:

- `roles` is currently expected as a comma-separated string
- `sub` is used as the authenticated principal name
- `iat` is required
- `exp` is enforced by the JWT parser

## Verification Performed

### Automated tests

Targeted test command:

```bash
mvn "-Dtest=JwtTokenValidatorTest,AuthenticationSecurityTest" test
```

Verified passing:

- `JwtTokenValidatorTest`: 6 tests
- `AuthenticationSecurityTest`: 4 tests
- total: 10 tests, 0 failures, 0 errors

### Real HTTP verification

The application was also started with a generated RSA keypair and exercised over HTTP. Observed results:

- `GET /api/health` without token -> `200`
- `GET /api/admin/analytics/platform-activity` without token -> `401`
- `GET /api/admin/analytics/platform-activity` with invalid token -> `401`
- `GET /api/admin/analytics/platform-activity` with valid trusted token -> `200`

## Important Limitations / Not Yet Added

This implementation does **not** currently add:

- refresh/logout endpoints
- revocation / blacklist checks
- JWKS auto-refresh
- issuer / audience claim enforcement
- fine-grained role/permission authorization rules

So the current system is best described as:

**local username/password authentication with signed JWT bearer tokens and session tracking**

## Operational Notes

### To enable in an environment

1. Generate a JWT key pair.
2. Store the Base64-encoded X.509 public key in `auth.jwt.public-key`.
3. Store the Base64-encoded PKCS#8 private key in `auth.jwt.private-key`.
4. Keep `auth.jwt.enabled=true`.

### To disable temporarily

```yaml
auth:
  jwt:
    enabled: false
```

When disabled, the validator returns unauthenticated tokens and the security config permits all requests.

## Summary

The authentication work added to Toadstool provides:

- Spring Security integration
- a custom JWT authentication filter
- local verification of externally issued JWTs
- stateless request authentication
- public-key-based signature verification
- protection for all non-public endpoints
- automated unit and integration coverage proving anonymous requests are rejected and valid signed tokens are accepted
