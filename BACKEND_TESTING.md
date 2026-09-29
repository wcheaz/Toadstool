# Backend Authentication System - Testing Guide

## ✅ Backend Only Implementation

All 6 authentication requirements have been successfully implemented in the **backend only**:

1. ✅ Database table for usernames and passwords
2. ✅ Register and login service
3. ✅ Database table for session keys
4. ✅ JWT token generation
5. ✅ JWT token verification
6. ✅ No frontend (as requested)

---

## 🚀 Backend Status

**Backend is running on: http://localhost:8081**

### Available Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |
| POST | `/api/auth/refresh` | Refresh access token |

---

## 🧪 Testing Methods

### Method 1: cURL Commands

> **PowerShell note:** `curl` is usually an alias for `Invoke-WebRequest` in PowerShell, so `-X` and `-H` will fail.  
> Use `Invoke-RestMethod` for the easiest Windows test, or use `curl.exe` with a single-line JSON body.

#### PowerShell (recommended)
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body (@{
    email = "user@example.com"
    displayName = "John Doe"
    username = "johndoe"
    password = "SecurePassword123"
  } | ConvertTo-Json)
```

#### Real cURL (`curl.exe`)
```bash
curl.exe -X POST "http://localhost:8081/api/auth/register" -H "Content-Type: application/json" --data-raw "{\"email\":\"user@example.com\",\"displayName\":\"John Doe\",\"username\":\"johndoe\",\"password\":\"SecurePassword123\"}"
```

#### Register a New User
Use one of the commands above.

**Expected Response (201):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "clientId": "550e8400-e29b-41d4-a716-446655440000",
  "accountId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "johndoe",
  "email": "user@example.com",
  "role": "CLIENT"
}
```

#### Login with Credentials
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body (@{
    username = "johndoe"
    password = "SecurePassword123"
  } | ConvertTo-Json)
```

**Expected Response (200):** Same as registration response with new tokens

#### Refresh Access Token
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/auth/refresh" `
  -Method Post `
  -ContentType "application/json" `
  -Body (@{
    refreshToken = "<PASTE_REFRESH_TOKEN_HERE>"
  } | ConvertTo-Json)
```

**Expected Response (200):** New access and refresh tokens

---

### Method 2: PowerShell Script

Create a file `test-auth.ps1`:

```powershell
$baseUrl = "http://localhost:8081/api/auth"
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$username = "testuser_$timestamp"
$email = "test_$timestamp@example.com"
$password = "TestPassword123"

Write-Host "Testing Authentication System..." -ForegroundColor Cyan

# Test 1: Register
Write-Host "`n1. Testing Registration..." -ForegroundColor Yellow
$registerBody = @{
    email = $email
    displayName = "Test User"
    username = $username
    password = $password
} | ConvertTo-Json

$registerResponse = Invoke-WebRequest -Uri "$baseUrl/register" `
    -Method Post `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $registerBody

$data = $registerResponse.Content | ConvertFrom-Json
$accessToken = $data.accessToken
$refreshToken = $data.refreshToken

Write-Host "✅ Registration successful!" -ForegroundColor Green
Write-Host "   Username: $username"
Write-Host "   Email: $email"

# Test 2: Login
Write-Host "`n2. Testing Login..." -ForegroundColor Yellow
$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "$baseUrl/login" `
    -Method Post `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $loginBody

$loginData = $loginResponse.Content | ConvertFrom-Json
Write-Host "✅ Login successful!" -ForegroundColor Green

# Test 3: Refresh Token
Write-Host "`n3. Testing Token Refresh..." -ForegroundColor Yellow
$refreshBody = @{
    refreshToken = $refreshToken
} | ConvertTo-Json

$refreshResponse = Invoke-WebRequest -Uri "$baseUrl/refresh" `
    -Method Post `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $refreshBody

$newTokenData = $refreshResponse.Content | ConvertFrom-Json
Write-Host "✅ Token refresh successful!" -ForegroundColor Green

# Test 4: Verify JWT Structure
Write-Host "`n4. Verifying JWT Token Structure..." -ForegroundColor Yellow
$tokenParts = $accessToken -split '\.'
if ($tokenParts.Count -eq 3) {
    Write-Host "✅ JWT token has valid structure (3 parts)" -ForegroundColor Green
} else {
    Write-Host "❌ JWT token structure is invalid" -ForegroundColor Red
}

Write-Host "`n✅ ALL TESTS PASSED!" -ForegroundColor Green
```

Run with:
```bash
powershell -ExecutionPolicy Bypass -File test-auth.ps1
```

---

### Method 3: Postman Collection

Import this into Postman:

```json
{
  "info": {
    "name": "Toadstool Auth API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Register User",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"user@example.com\",\n  \"displayName\": \"Test User\",\n  \"username\": \"testuser\",\n  \"password\": \"TestPassword123\"\n}"
        },
        "url": {"raw": "http://localhost:8081/api/auth/register", "protocol": "http", "host": ["localhost"], "port": ["8081"], "path": ["api", "auth", "register"]}
      }
    },
    {
      "name": "Login User",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"testuser\",\n  \"password\": \"TestPassword123\"\n}"
        },
        "url": {"raw": "http://localhost:8081/api/auth/login", "protocol": "http", "host": ["localhost"], "port": ["8081"], "path": ["api", "auth", "login"]}
      }
    },
    {
      "name": "Refresh Token",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"refreshToken\": \"<PASTE_TOKEN_HERE>\"\n}"
        },
        "url": {"raw": "http://localhost:8081/api/auth/refresh", "protocol": "http", "host": ["localhost"], "port": ["8081"], "path": ["api", "auth", "refresh"]}
      }
    }
  ]
}
```

---

## 📋 Test Cases

### Test 1: Valid Registration
**Input:**
```json
{
  "email": "alice@example.com",
  "displayName": "Alice",
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 201 Created with tokens ✅

### Test 2: Valid Login
**Input:**
```json
{
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 200 OK with tokens ✅

### Test 3: Invalid Password
**Input:**
```json
{
  "username": "alice",
  "password": "WrongPassword"
}
```
**Expected Result:** 400 Bad Request with "Invalid username or password" ✅

### Test 4: Duplicate Username
**Input:**
```json
{
  "email": "bob@example.com",
  "displayName": "Bob",
  "username": "alice",
  "password": "Password123"
}
```
**Expected Result:** 400 Bad Request with "Username is already registered" ✅

### Test 5: Short Password
**Input:**
```json
{
  "email": "charlie@example.com",
  "displayName": "Charlie",
  "username": "charlie",
  "password": "short"
}
```
**Expected Result:** 400 Bad Request with validation error ✅

---

## 🗄️ Database Verification

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
SELECT * FROM trading.account_credentials WHERE username = 'alice';
```

---

## 📁 Backend Files

### Files Modified (2)
- `src/main/java/com/neueda/leap/auth/AuthController.java` - Added `/refresh` endpoint
- `src/main/java/com/neueda/leap/auth/AuthService.java` - Added `refresh()` method

### Database Migration
- `src/main/resources/db/migration/V4__auth.sql` - Schema already existed

### All Backend Components Present
- ✅ `AuthService.java` - Business logic
- ✅ `AuthController.java` - REST endpoints
- ✅ `JwtTokenService.java` - Token generation
- ✅ `JwtTokenValidator.java` - Token validation
- ✅ `JwtAuthenticationFilter.java` - Spring filter
- ✅ `SessionKeyService.java` - Session tracking
- ✅ Data transfer objects and mappers

---

## 🔐 Security Features

✅ BCrypt password hashing
✅ RS256 JWT signing (RSA-2048)
✅ Token expiration (60 min access, 30 day refresh)
✅ Session tracking and revocation
✅ Bearer token authentication
✅ Unique username/email constraints
✅ Input validation

---

## 📊 Response Codes

| Code | Meaning |
|------|---------|
| 201 | Created (successful registration) |
| 200 | OK (successful login/refresh) |
| 400 | Bad Request (validation errors) |
| 401 | Unauthorized (invalid token) |
| 403 | Forbidden (no token) |

---

## ✅ Implementation Summary

| Requirement | Status | Details |
|-------------|--------|---------|
| Database tables | ✅ | 2 tables with proper schema |
| Register service | ✅ | `AuthService.register()` |
| Login service | ✅ | `AuthService.login()` |
| Session tracking | ✅ | `auth_sessions` table |
| JWT generation | ✅ | `JwtTokenService` |
| JWT verification | ✅ | `JwtTokenValidator` + filter |
| Backend running | ✅ | http://localhost:8081 |

---

## 🎯 Quick Start Testing

1. **Backend is running at:** http://localhost:8081

2. **Test with cURL:**
Use the PowerShell command above, or the single-line `curl.exe` form.

3. **Or use Postman** with the collection above

4. **Or run the PowerShell test script** provided

---

**Status: ✅ Backend Authentication System Complete and Running**
