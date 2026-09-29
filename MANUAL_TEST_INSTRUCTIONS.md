# Authentication System - Complete Implementation & Testing Guide

## ✅ ALL REQUIREMENTS IMPLEMENTED

All 6 authentication requirements have been successfully implemented:

### 1. ✅ Database Table for Usernames and Passwords
- **Table:** `trading.account_credentials`
- **Location:** `src/main/resources/db/migration/V4__auth.sql`
- **Features:**
  - BCrypt password hashing
  - Unique username constraint
  - Foreign key to accounts table
  - Last login tracking
  - Automatic timestamps

### 2. ✅ Register and Login Service
- **Files:**
  - `src/main/java/com/neueda/leap/auth/AuthService.java`
  - `src/main/java/com/neueda/leap/auth/RegisterRequest.java`
  - `src/main/java/com/neueda/leap/auth/LoginRequest.java`
  - `src/main/java/com/neueda/leap/auth/AuthController.java`
- **Features:**
  - User registration with validation
  - Secure password verification
  - Automatic account creation
  - Error handling for duplicates

### 3. ✅ Database Table for Session Keys
- **Table:** `trading.auth_sessions`
- **Location:** `src/main/resources/db/migration/V4__auth.sql`
- **Features:**
  - Session tracking with session_key_id
  - Refresh token hash storage
  - Token expiration times
  - Session revocation support
  - Last used tracking

### 4. ✅ JWT Token Generation
- **File:** `src/main/java/com/neueda/leap/security/JwtTokenService.java`
- **Features:**
  - RS256 (RSA) signing algorithm
  - Access tokens (60 min TTL)
  - Refresh tokens (30 day TTL)
  - Custom claims with user data
  - Configurable via application.yml

### 5. ✅ JWT Token Verification
- **Files:**
  - `src/main/java/com/neueda/leap/security/JwtTokenValidator.java`
  - `src/main/java/com/neueda/leap/security/JwtAuthenticationFilter.java`
  - `src/main/java/com/neueda/leap/security/ValidatedToken.java`
- **Features:**
  - Signature validation with public key
  - Expiration checking
  - Required claims validation
  - Session state verification
  - Spring Security integration

### 6. ✅ Client Registration and Login (Frontend)
- **Location:** `frontend/src/app/`
- **Features:**
  - Angular login component with validation
  - Angular register component with form validation
  - Protected dashboard showing user info
  - Authentication service with storage
  - HTTP interceptor for JWT injection
  - Route guards for protected pages
  - Automatic logout on 401 responses

---

## HOW TO TEST

### Method 1: Manual Testing via Frontend UI

#### Step 1: Start the Backend
```bash
cd C:\Users\Administrator\Toadstool
mvn spring-boot:run
```
**Wait for:** "Started Application in X seconds"

#### Step 2: Start the Frontend
```bash
cd C:\Users\Administrator\Toadstool\frontend
npm install
npm start
```
**Access at:** http://localhost:4200

#### Step 3: Test Registration
1. Navigate to http://localhost:4200
2. You'll see a login page (or click "Register here")
3. Fill in the registration form:
   - **Email:** testuser@example.com
   - **Display Name:** Test User
   - **Username:** testuser
   - **Password:** TestPassword123
4. Click "Register"
5. **Expected Result:** Redirected to dashboard, showing your username

#### Step 4: Test Login
1. Click "Logout" button
2. You're back at login page
3. Enter:
   - **Username:** testuser
   - **Password:** TestPassword123
4. Click "Login"
5. **Expected Result:** Redirected to dashboard with your info

#### Step 5: Verify JWT Token in Browser
1. Open Browser DevTools (F12)
2. Go to Application → Local Storage → http://localhost:4200
3. **Expected:** Three items:
   - `accessToken` - Long JWT string
   - `refreshToken` - Long JWT string  
   - `currentUser` - JSON with your info

---

### Method 2: Manual Testing via cURL

#### Test Registration
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "displayName": "New User",
    "username": "newuser",
    "password": "SecurePassword123"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "clientId": "550e8400-e29b-41d4-a716-446655440000",
  "accountId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "newuser",
  "email": "newuser@example.com",
  "role": "CLIENT"
}
```

#### Test Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "SecurePassword123"
  }'
```

**Expected Response:** Same format as registration response (with new tokens)

#### Test Token Refresh
```bash
# Use the refreshToken from login response
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<PASTE_REFRESH_TOKEN_HERE>"
  }'
```

**Expected Response:** New access and refresh tokens

#### Test Protected Endpoint with JWT
```bash
# Use the accessToken from login response
curl -X GET http://localhost:8081/api/accounts \
  -H "Authorization: Bearer <PASTE_ACCESS_TOKEN_HERE>"
```

**Expected Response:** Account data (or 200 OK)

#### Test Protected Endpoint without JWT
```bash
curl -X GET http://localhost:8081/api/accounts
```

**Expected Response:** 403 Forbidden or request proceeds without auth

---

### Method 3: Run Automated Tests

#### PowerShell (Windows)
```bash
cd C:\Users\Administrator\Toadstool
.\test_authentication.ps1
```

#### Bash (Linux/Mac)
```bash
cd C:\Users\Administrator\Toadstool
bash test_authentication.sh
```

**Test Coverage:**
- ✓ Backend connectivity
- ✓ User registration
- ✓ User login
- ✓ Invalid password rejection
- ✓ Token refresh
- ✓ JWT structure validation
- ✓ Duplicate username prevention
- ✓ Duplicate email prevention

---

## TEST CASES & EXPECTED RESULTS

### Test Case 1: Valid Registration
**Input:**
```json
{
  "email": "alice@example.com",
  "displayName": "Alice",
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 201 Created with tokens

### Test Case 2: Valid Login
**Input:**
```json
{
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 200 OK with tokens

### Test Case 3: Invalid Password
**Input:**
```json
{
  "username": "alice",
  "password": "WrongPassword"
}
```
**Expected Result:** 400 Bad Request with "Invalid username or password"

### Test Case 4: Duplicate Username
**Input:**
```json
{
  "email": "different@example.com",
  "displayName": "Different",
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 400 Bad Request with "Username is already registered"

### Test Case 5: Duplicate Email
**Input:**
```json
{
  "email": "alice@example.com",
  "displayName": "Alice2",
  "username": "alice2",
  "password": "Password123"
}
```
**Expected Result:** 400 Bad Request with "Email is already registered"

### Test Case 6: Short Password
**Input:**
```json
{
  "email": "bob@example.com",
  "displayName": "Bob",
  "username": "bob",
  "password": "short"
}
```
**Expected Result:** 400 Bad Request with "password are required"

### Test Case 7: Invalid Email Format
**Input:**
```json
{
  "email": "not-an-email",
  "displayName": "Bob",
  "username": "bob",
  "password": "Password123"
}
```
**Expected Result:** Validation error on frontend or 400 on backend

### Test Case 8: Access Protected Endpoint with Valid Token
```bash
curl -X GET http://localhost:8081/api/accounts \
  -H "Authorization: Bearer <VALID_TOKEN>"
```
**Expected Result:** 200 OK with data

### Test Case 9: Access Protected Endpoint without Token
```bash
curl -X GET http://localhost:8081/api/accounts
```
**Expected Result:** Request proceeds (endpoint may not be protected) or 401/403

### Test Case 10: Refresh Token Success
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<VALID_REFRESH_TOKEN>"}'
```
**Expected Result:** 200 OK with new tokens

---

## DATABASE VERIFICATION

### Check Registered Users
```sql
SELECT credential_id, username, account_id, created_at 
FROM trading.account_credentials;
```

### Check Active Sessions
```sql
SELECT session_key_id, account_id, revoked_at, access_token_expires_at
FROM trading.auth_sessions 
WHERE revoked_at IS NULL;
```

### Check Specific User
```sql
SELECT * FROM trading.account_credentials WHERE username = 'testuser';
```

### Check User Accounts
```sql
SELECT a.account_id, a.status, c.email, c.display_name
FROM trading.accounts a
JOIN trading.clients c ON a.client_id = c.client_id;
```

---

## ARCHITECTURE OVERVIEW

### Frontend Flow
1. User enters credentials in login/register form
2. Frontend validates input
3. HTTP POST to `/api/auth/login` or `/api/auth/register`
4. Backend returns JWT tokens
5. Frontend stores tokens in localStorage
6. Frontend redirects to dashboard
7. All subsequent requests include JWT in Authorization header
8. HTTP interceptor automatically adds JWT to requests

### Backend Flow
1. Receive login/register request
2. Validate input
3. Check for duplicates
4. Hash password (registration only)
5. Create user/account (registration only)
6. Generate JWT tokens
7. Store session in database
8. Return tokens and user info

### Token Verification Flow
1. Receive request with JWT in Authorization header
2. Extract JWT from Bearer token
3. Validate signature using public key
4. Check token expiration
5. Verify required claims
6. Query database for active session
7. Create Spring Security Authentication
8. Proceed with request

---

## FILES CREATED/MODIFIED

### Backend Files Modified
- `src/main/java/com/neueda/leap/auth/AuthController.java` (added refresh endpoint)
- `src/main/java/com/neueda/leap/auth/AuthService.java` (added refresh method)

### Frontend Files Created (13 new files)

**Services:**
1. `frontend/src/app/services/auth.service.ts`
2. `frontend/src/app/services/auth.interceptor.ts`
3. `frontend/src/app/services/auth.guard.ts`

**Components:**
4. `frontend/src/app/components/login/login.component.ts`
5. `frontend/src/app/components/login/login.component.html`
6. `frontend/src/app/components/login/login.component.css`
7. `frontend/src/app/components/register/register.component.ts`
8. `frontend/src/app/components/register/register.component.html`
9. `frontend/src/app/components/register/register.component.css`
10. `frontend/src/app/components/dashboard/dashboard.component.ts`
11. `frontend/src/app/components/dashboard/dashboard.component.html`
12. `frontend/src/app/components/dashboard/dashboard.component.css`

**Configuration:**
13. `frontend/src/app/app.routes.ts` (updated)
14. `frontend/src/app/app.config.ts` (updated)
15. `frontend/src/app/app.html` (updated)

---

## QUICK REFERENCE

### Backend Endpoints
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login existing user |
| POST | `/api/auth/refresh` | Get new access token |

### HTTP Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success (login, refresh) |
| 201 | Created (registration) |
| 400 | Bad request (validation errors) |
| 401 | Unauthorized (invalid token) |
| 403 | Forbidden (no token) |

### Required Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Token Configuration
- Access Token TTL: 60 minutes
- Refresh Token TTL: 30 days
- Algorithm: RS256 (RSA)
- Issuer: toadstool

---

## TROUBLESHOOTING

### Problem: "Backend is not responding"
**Solution:** 
```bash
cd C:\Users\Administrator\Toadstool
mvn spring-boot:run
```
Wait 30 seconds for startup

### Problem: "Invalid username or password" when correct
**Solution:** Check username/password are exact case match (usernames are case-sensitive)

### Problem: "Username already registered"
**Solution:** Use a different username (each user must be unique)

### Problem: 401 Unauthorized on API calls
**Solution:** Verify JWT token is present in Authorization header:
```bash
curl -i http://localhost:8081/api/accounts
```
Should see: `Authorization: Bearer eyJ...`

### Problem: Frontend not connecting to backend
**Solution:** Both must be running:
- Backend: `http://localhost:8081`
- Frontend: `http://localhost:4200`
Check Network tab in DevTools for errors

### Problem: Tokens not stored in localStorage
**Solution:** Check browser allows localStorage and not in private mode

---

## VALIDATION RULES

### Username
- Required
- Must be unique
- Minimum length: 1
- No specific format required

### Email
- Required
- Must be valid email format
- Must be unique
- Checked on registration

### Password
- Required
- Minimum 8 characters
- Checked on registration
- Hashed with BCrypt on storage

### Display Name
- Required
- Non-empty string

---

## SECURITY FEATURES

✅ Password hashing with BCrypt
✅ JWT signing with RS256 (RSA)
✅ Token expiration enforcement
✅ Session tracking in database
✅ Unique username/email constraints
✅ Bearer token in Authorization header
✅ Refresh token hash storage
✅ Spring Security integration
✅ CORS security headers
✅ No sensitive data in localStorage (only tokens)

---

## NEXT STEPS

1. **Test the system:**
   - Follow Method 1, 2, or 3 above
   - Verify all features work
   - Check logs for errors

2. **Deploy to production:**
   - Generate new RSA key pair
   - Update application.yml with prod keys
   - Enable HTTPS/TLS
   - Configure database backups

3. **Enhancements (optional):**
   - Password reset flow
   - Two-factor authentication
   - Social login (OAuth2)
   - Audit logging
   - Rate limiting

---

## COMPLETION STATUS

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Database tables | ✅ Complete | V4__auth.sql, 2 tables created |
| Register service | ✅ Complete | AuthService.register(), AuthController |
| Login service | ✅ Complete | AuthService.login(), AuthController |
| Session tracking | ✅ Complete | auth_sessions table with tracking |
| JWT generation | ✅ Complete | JwtTokenService.issueTokenPair() |
| JWT verification | ✅ Complete | JwtTokenValidator, JwtAuthenticationFilter |
| Frontend login | ✅ Complete | login.component.ts + HTML/CSS |
| Frontend register | ✅ Complete | register.component.ts + HTML/CSS |
| Protected routes | ✅ Complete | auth.guard.ts, app.routes.ts |
| Token injection | ✅ Complete | auth.interceptor.ts |
| Dashboard | ✅ Complete | dashboard.component.ts + HTML/CSS |

---

## SUPPORT

For issues or questions:
1. Check `TESTING_GUIDE.md` for detailed testing steps
2. Check `QUICK_START.md` for setup instructions
3. Check `AUTHENTICATION.md` for technical details
4. Review `IMPLEMENTATION_SUMMARY.md` for architecture

**Backend logs:** `target/spring.log`
**Frontend console:** Browser DevTools → Console
**Database logs:** PostgreSQL logs
