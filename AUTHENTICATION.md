# Authentication Overview

This project implements secure client registration and sign-in, short-lived JWT-based sessions, ownership enforcement, server-side session revocation, and append-only audit records.

## Requirement coverage

### Register users, securely hash passwords, authenticate credentials, and issue JWTs

Implemented.

- Endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
  - `POST /api/auth/validate`
- Main auth controller: [AuthController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthController.java)
- Main auth service: [AuthService.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthService.java)
- Credential storage: [V4__auth.sql](C:/Users/Administrator/Toadstool/src/main/resources/db/migration/V4__auth.sql)
  - `trading.account_credentials`
- Passwords are stored as hashes and verified with Spring Security's password encoder.
- Register and login return:
  - `accessToken`
  - `expiresIn`
  - `clientId`
  - `accountId`
  - profile data

### BR-02: A client may only view and act on their own positions, cash and order history

Implemented.

- JWT `sub` contains the authenticated client ID.
- JWT also includes:
  - `username`
  - `clientId`
  - `accountId`
  - `email`
  - `roles`
  - `tokenType`
  - session key ID (`jti`)
- JWT validation: [JwtTokenValidator.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtTokenValidator.java)
- Request authentication filter: [JwtAuthenticationFilter.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtAuthenticationFilter.java)
- Ownership helper: [SecurityAccess.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/SecurityAccess.java)

Ownership checks are enforced on protected client/account/order/fill/audit endpoints, including:

- [ClientController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/ClientController.java)
- [AccountController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/AccountController.java)
- [OrderController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/OrderController.java)
- [FillController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/FillController.java)
- [TradeEventController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/TradeEventController.java)
- [LoginEventController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/LoginEventController.java)

Admin-only restrictions are enforced on:

- [AdminAnalyticsController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/AdminAnalyticsController.java)
- [AdminUserController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/AdminUserController.java)
- admin write operations in [InstrumentController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/InstrumentController.java)
- legacy admin lookup endpoint in [LoginController.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/controller/LoginController.java)

### BR-03: A client’s signed-in session should be time-limited and revocable

Implemented without refresh tokens.

- Sessions use short-lived access JWTs.
- Default access-token TTL is 15 minutes in [JwtProperties.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/JwtProperties.java).
- Active sessions are tracked server-side in `trading.auth_sessions`.
- Every authenticated API request checks that the session key in the JWT is still active.
- `POST /api/auth/logout` revokes the active server-side session.
- When the access token expires, the user must sign in again.

Related files:

- [SessionKeyService.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/SessionKeyService.java)
- [AuthSessionMapper.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/auth/AuthSessionMapper.java)
- [V5__remove_refresh_token_requirement.sql](C:/Users/Administrator/Toadstool/src/main/resources/db/migration/V5__remove_refresh_token_requirement.sql)

### Section 9.3: Security and confidentiality

Implemented in the application layer as follows.

- HTTPS:
  - HTTPS redirect is enabled through [SecurityConfig.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/security/SecurityConfig.java)
  - controlled by `auth.jwt.require-https`
  - enabled by default in [application.yml.example](C:/Users/Administrator/Toadstool/src/main/resources/application.yml.example)
- Secure token storage:
  - frontend token persistence uses session storage in [auth-storage.ts](C:/Users/Administrator/Toadstool/frontend/src/app/auth-storage.ts)
  - logout clears both session-storage and legacy local-storage keys
- Appropriate authorization:
  - every non-public API route requires authentication
  - ownership and role checks are enforced on protected routes
- Least privilege:
  - admin routes require `ROLE_ADMIN`
  - client routes allow access only to owned resources

### BR-14 and BR-15: Permanent audit records

Implemented and extended.

- Audit tables remain append-only in [V1__init.sql](C:/Users/Administrator/Toadstool/src/main/resources/db/migration/V1__init.sql):
  - `trading.trade_events`
  - `trading.login_events`
- Order submission records a permanent trade event in:
  - [OrderService.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/service/OrderService.java)
  - [TradeEventService.java](C:/Users/Administrator/Toadstool/src/main/java/com/neueda/leap/service/TradeEventService.java)
- Recorded order-event details include:
  - associated client ID
  - timestamp (`occurred_at`)
  - authenticated user ID in event details
  - account ID
  - instrument ID
  - side
  - quantity
  - idempotency key

## Manual test steps

### 1. Start the application

1. Start PostgreSQL with the configured database.
2. Start the backend:
   - `mvn spring-boot:run`
3. Start the frontend:
   - from [frontend/](C:/Users/Administrator/Toadstool/frontend)
   - `npm start`

### 2. Register and log in

1. Open the frontend.
2. Go to `/register`.
3. Register a new user.
4. Confirm you land on the homepage.
5. Log out.
6. Go to `/login`.
7. Log in with the same username and password.
8. Confirm you land on the homepage again.

Expected:

- registration succeeds
- login succeeds
- passwords are never returned by the API
- browser storage uses session storage for the active login

### 3. Validate the JWT subject and claims

1. Copy the access token from browser session storage.
2. Call:
   - `POST /api/auth/validate`
3. Body:
   - `{ "token": "YOUR_ACCESS_TOKEN" }`

Expected:

- `authenticated` is `true`
- `subject` equals the returned `clientId`
- `username` matches the logged-in username
- `accountId` matches the authenticated account
- `tokenType` is `ACCESS`

### 4. Verify ownership enforcement

1. Register User A and keep User A's access token.
2. Register User B and note User B's `clientId` and `accountId`.
3. Using User A's token, call:
   - `GET /api/clients/{userBClientId}`
   - `GET /api/accounts/{userBAccountId}`
   - `GET /api/accounts/{userBAccountId}/orders`

Expected:

- each request returns `403 Forbidden`

4. Using User A's token, call the same endpoints with User A's own IDs.

Expected:

- the owned-resource requests succeed

### 5. Verify logout/session revocation

1. Call:
   - `POST /api/auth/logout`
2. Send the current access token in the `Authorization` header.
3. Retry a protected request, for example:
   - `GET /api/clients/{yourClientId}`

Expected:

- logout returns `204 No Content`
- the revoked access token no longer works
- the protected request returns `401`

### 6. Verify session expiry behavior

1. Log in and note `expiresIn`.
2. Wait for the token to expire, or temporarily lower `auth.jwt.access-token-ttl-minutes` in local config and restart.
3. Retry a protected request with the expired token.

Expected:

- the expired token returns `401`
- the user must sign in again

### 7. Verify audit recording for orders

1. Log in as a client.
2. Place an order through:
   - `POST /api/accounts/{yourAccountId}/orders`
3. Then request:
   - `GET /api/orders/{orderId}/events`

Expected:

- an order `SUBMITTED` event exists
- it contains the associated client and timestamp
- the event details include the authenticated user ID and order metadata

### 8. Verify HTTPS behavior

1. Ensure `auth.jwt.require-https=true` in runtime config for secured environments.
2. Make an HTTP request to the backend.

Expected:

- the application redirects the request to HTTPS

For local non-SSL development only, disable this setting before starting the backend.

## What was verified

- Targeted backend tests passed:
  - [AuthenticationSecurityTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/security/AuthenticationSecurityTest.java)
  - [JwtTokenValidatorTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/security/JwtTokenValidatorTest.java)
  - [OrderServiceTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/service/OrderServiceTest.java)
  - [AccountControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/AccountControllerTest.java)
  - [ClientControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/ClientControllerTest.java)
  - [OrderControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/OrderControllerTest.java)
  - [FillControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/FillControllerTest.java)
  - [TradeEventControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/TradeEventControllerTest.java)
  - [LoginEventControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/LoginEventControllerTest.java)
  - [InstrumentControllerTest.java](C:/Users/Administrator/Toadstool/src/test/java/com/neueda/leap/controller/InstrumentControllerTest.java)
- Frontend production build passed:
  - [frontend/](C:/Users/Administrator/Toadstool/frontend)
