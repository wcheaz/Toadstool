# Quick Start: Building and Testing Authentication

## One-Command Quick Start

### Terminal 1 - Start Backend
```bash
cd C:\Users\Administrator\Toadstool
mvn spring-boot:run
```
Backend will be available at: http://localhost:8081

### Terminal 2 - Start Frontend
```bash
cd C:\Users\Administrator\Toadstool\frontend
npm install
npm start
```
Frontend will be available at: http://localhost:4200

## Quick Test Cases

### 1. Test Registration (Frontend)
```
1. Go to http://localhost:4200
2. Click "Register here"
3. Fill form:
   Email: test@example.com
   Name: Test User
   Username: testuser
   Password: testpass123
4. Click Register
5. You should see dashboard with your username
```

### 2. Test Login (Frontend)
```
1. Logout first
2. Go to http://localhost:4200/login
3. Username: testuser
4. Password: testpass123
5. Click Login
6. You should see dashboard
```

### 3. Test API Directly (cURL)
```bash
# Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@test.com",
    "displayName": "Test",
    "username": "testuser2",
    "password": "password123"
  }'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser2",
    "password": "password123"
  }'

# Refresh token (use refreshToken from login response)
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<REFRESH_TOKEN>"}'
```

## Files Changed/Added

### Backend
- ✅ `AuthController.java` - Added `/refresh` endpoint
- ✅ `AuthService.java` - Added `refresh()` method
- ✅ `V4__auth.sql` - Database migration (already existed)

### Frontend (New)
- ✅ `services/auth.service.ts` - Authentication service
- ✅ `services/auth.interceptor.ts` - HTTP interceptor for JWT
- ✅ `services/auth.guard.ts` - Route protection
- ✅ `components/login/` - Login component
- ✅ `components/register/` - Register component
- ✅ `components/dashboard/` - Protected dashboard
- ✅ `app.routes.ts` - Routes configuration
- ✅ `app.config.ts` - HTTP interceptor setup

## Key Features

### Backend Authentication
- Secure password hashing with BCrypt
- JWT generation with RSA-256
- Refresh token mechanism
- Session tracking in database
- Token validation on all protected endpoints

### Frontend Authentication
- Login/Register components
- Automatic JWT injection in requests
- Protected routes with guards
- Token storage in localStorage
- Auto-logout on 401 responses
- Dashboard showing user info

## Environment Configuration
- Backend: Spring Boot on port 8081
- Frontend: Angular on port 4200
- Database: PostgreSQL
- JWT: RSA-256 signing
- Access Token TTL: 60 minutes
- Refresh Token TTL: 30 days

## Important Notes

1. **Password Requirements:**
   - Minimum 8 characters
   - Plain text password sent to backend (use HTTPS in production)

2. **Token Storage:**
   - Stored in browser localStorage
   - Cleared on logout
   - Automatically included in API requests

3. **CORS (if needed):**
   - Frontend and backend run on different ports
   - Adjust Spring Security CORS configuration if needed

4. **Database:**
   - Automatic schema creation via Flyway
   - Tables: `account_credentials`, `auth_sessions`
   - Indexed for performance

## Troubleshooting

### "Invalid username or password"
- Verify username and password are correct
- Check database has the user: `SELECT * FROM trading.account_credentials WHERE username = 'testuser';`

### "Username/Email already registered"
- Username and email must be unique
- Use different credentials for each test user

### 401 Unauthorized on API calls
- JWT token may have expired
- Token not being sent in Authorization header
- Check browser DevTools Network tab for Authorization header

### Frontend not connecting to backend
- Verify backend is running on http://localhost:8081
- Check Network tab for CORS errors
- Backend may need CORS configuration

## Next Steps

1. Deploy to production environment
2. Implement password reset functionality
3. Add two-factor authentication
4. Add role-based access control (RBAC)
5. Implement logout from all sessions
6. Add audit logging for authentication events
