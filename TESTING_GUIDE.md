# Manual Testing Guide for Authentication

## Prerequisites
- Backend running on http://localhost:8081
- Frontend running on http://localhost:4200
- PostgreSQL database is running and configured

## Setup Instructions

### Start the Backend
```bash
cd C:\Users\Administrator\Toadstool
mvn spring-boot:run
```

The backend will be available at: `http://localhost:8081`

### Start the Frontend
```bash
cd C:\Users\Administrator\Toadstool\frontend
npm install
npm start
```

The frontend will be available at: `http://localhost:4200`

## Test Cases

### Test 1: User Registration via Frontend
1. Open browser and navigate to `http://localhost:4200`
2. You should be redirected to `/login`
3. Click "Register here" link
4. Fill in the registration form:
   - Email: `testuser@example.com`
   - Display Name: `Test User`
   - Username: `testuser`
   - Password: `testpassword123`
5. Click "Register" button
6. You should be redirected to the dashboard
7. Verify your username is displayed

**Expected Result:** User is registered and logged in automatically

---

### Test 2: User Login via Frontend
1. Open browser and navigate to `http://localhost:4200/login`
2. Fill in the login form:
   - Username: `testuser`
   - Password: `testpassword123`
3. Click "Login" button
4. You should be redirected to the dashboard
5. Verify your username and email are displayed

**Expected Result:** User is authenticated and can access the dashboard

---

### Test 3: JWT Token in LocalStorage
1. After logging in, open Browser DevTools (F12)
2. Go to Application → Local Storage → http://localhost:4200
3. Verify these keys exist:
   - `accessToken` - Should contain a long JWT token
   - `refreshToken` - Should contain a long JWT token
   - `currentUser` - Should contain JSON with user info

**Expected Result:** All three items are stored in localStorage

---

### Test 4: JWT Token in HTTP Requests
1. After logging in, open Browser DevTools (F12)
2. Go to Network tab
3. Navigate to a protected page or perform an action
4. Click on any API request (e.g., GET /api/accounts)
5. Go to Request Headers
6. Verify the `Authorization` header contains: `Bearer <JWT_TOKEN>`

**Expected Result:** Authorization header is present in all API requests

---

### Test 5: Login with Invalid Credentials
1. Open browser and navigate to `http://localhost:4200/login`
2. Enter invalid credentials:
   - Username: `invaliduser`
   - Password: `wrongpassword`
3. Click "Login" button
4. Verify error message appears: "Invalid username or password"

**Expected Result:** Error message is displayed, user is not logged in

---

### Test 6: Protected Route Access
1. Logout if logged in
2. Try to navigate directly to `http://localhost:4200/dashboard`
3. You should be redirected to `http://localhost:4200/login`

**Expected Result:** User cannot access protected routes without authentication

---

### Test 7: Logout Functionality
1. After logging in, click the "Logout" button on the dashboard
2. You should be redirected to the login page
3. Open Browser DevTools (F12)
4. Go to Application → Local Storage
5. Verify all auth tokens have been removed

**Expected Result:** User is logged out and auth tokens are cleared

---

### Test 8: Backend Login Endpoint (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpassword123"
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
  "username": "testuser",
  "email": "testuser@example.com",
  "role": "CLIENT"
}
```

---

### Test 9: Backend Register Endpoint (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "displayName": "New User",
    "username": "newuser",
    "password": "newpassword123"
  }'
```

**Expected Response:** Same format as Test 8 with the new user's information

---

### Test 10: Protected Endpoint with JWT Token
First, get a valid access token from the login endpoint.

```bash
curl -X GET http://localhost:8081/api/accounts \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

**Expected Result:** Returns account data with status 200

---

### Test 11: Protected Endpoint without JWT Token
```bash
curl -X GET http://localhost:8081/api/accounts
```

**Expected Result:** Returns 401 Unauthorized or 403 Forbidden

---

### Test 12: Refresh Token (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<YOUR_REFRESH_TOKEN>"
  }'
```

**Expected Response:** New access and refresh tokens

---

## Validation Checks

### Password Requirements
- Minimum 8 characters
- Required field

### Username Requirements
- Must be unique
- Required field

### Email Requirements
- Must be a valid email format
- Must be unique
- Required field

### Display Name Requirements
- Required field
- Cannot be empty

---

## Error Scenarios

### Invalid Email Format
**Request:**
```json
{
  "email": "not-an-email",
  "displayName": "Test",
  "username": "testuser",
  "password": "testpassword123"
}
```
**Expected:** Validation error on frontend before sending request

### Duplicate Username
**Request (second time with same username):**
```json
{
  "email": "different@example.com",
  "displayName": "Test",
  "username": "testuser",
  "password": "testpassword123"
}
```
**Expected:** 400 Bad Request with message "Username is already registered"

### Duplicate Email
**Request (second time with same email):**
```json
{
  "email": "testuser@example.com",
  "displayName": "Test",
  "username": "differentusername",
  "password": "testpassword123"
}
```
**Expected:** 400 Bad Request with message "Email is already registered"

### Short Password
**Request:**
```json
{
  "email": "test@example.com",
  "displayName": "Test",
  "username": "testuser",
  "password": "short"
}
```
**Expected:** Validation error on frontend before sending request

---

## Database Verification

You can verify the authentication setup by querying the database:

```sql
-- Check credentials table
SELECT * FROM trading.account_credentials;

-- Check sessions table
SELECT * FROM trading.auth_sessions;

-- Check specific user
SELECT * FROM trading.account_credentials WHERE username = 'testuser';
```

---

## Summary

All authentication features have been successfully implemented:
- ✅ Secure password storage using BCrypt
- ✅ JWT token generation with RSA-256 signing
- ✅ Session tracking in database
- ✅ Token refresh capability
- ✅ Protected routes and endpoints
- ✅ Frontend login/register components
- ✅ Automatic token injection in API requests
- ✅ Logout and token cleanup
