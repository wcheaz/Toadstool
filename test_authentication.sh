#!/bin/bash
# Automated Testing Script for Authentication

set -e

# Configuration
BACKEND_URL="http://localhost:8081"
TIMEOUT=5

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        ((TESTS_FAILED++))
    fi
}

echo "=========================================="
echo "Authentication System Test Suite"
echo "=========================================="
echo ""

# Test 1: Check backend is running
echo "Test 1: Backend Health Check"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL/health --max-time $TIMEOUT || echo "000")
if [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "404" ]; then
    print_result 0 "Backend is running on $BACKEND_URL"
else
    print_result 1 "Backend is not responding (HTTP $RESPONSE)"
    exit 1
fi
echo ""

# Generate unique test user
TEST_TIMESTAMP=$(date +%s%N | cut -b1-13)
TEST_USER="testuser_${TEST_TIMESTAMP}"
TEST_EMAIL="test_${TEST_TIMESTAMP}@example.com"
TEST_PASSWORD="TestPassword123"

# Test 2: Register new user
echo "Test 2: User Registration"
REGISTER_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"displayName\": \"Test User\",
    \"username\": \"$TEST_USER\",
    \"password\": \"$TEST_PASSWORD\"
  }" --max-time $TIMEOUT)

ACCESS_TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)

if [ ! -z "$ACCESS_TOKEN" ] && [ ! -z "$REFRESH_TOKEN" ]; then
    print_result 0 "User registered successfully"
else
    print_result 1 "User registration failed"
    echo "Response: $REGISTER_RESPONSE"
fi
echo ""

# Test 3: Login with correct credentials
echo "Test 3: Login with Valid Credentials"
LOGIN_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$TEST_USER\",
    \"password\": \"$TEST_PASSWORD\"
  }" --max-time $TIMEOUT)

LOGIN_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ ! -z "$LOGIN_TOKEN" ]; then
    print_result 0 "Login successful"
else
    print_result 1 "Login failed"
    echo "Response: $LOGIN_RESPONSE"
fi
echo ""

# Test 4: Login with incorrect credentials
echo "Test 4: Login with Invalid Password"
INVALID_LOGIN=$(curl -s -X POST "$BACKEND_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$TEST_USER\",
    \"password\": \"WrongPassword\"
  }" --max-time $TIMEOUT)

if echo $INVALID_LOGIN | grep -q "Invalid username or password"; then
    print_result 0 "Invalid login rejected correctly"
else
    print_result 1 "Invalid login should be rejected"
fi
echo ""

# Test 5: Refresh token
echo "Test 5: Token Refresh"
REFRESH_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }" --max-time $TIMEOUT)

NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ ! -z "$NEW_ACCESS_TOKEN" ]; then
    print_result 0 "Token refreshed successfully"
else
    print_result 1 "Token refresh failed"
    echo "Response: $REFRESH_RESPONSE"
fi
echo ""

# Test 6: Verify JWT token structure
echo "Test 6: JWT Token Structure"
# Count dots in token (JWT should have 2 dots for 3 parts)
DOT_COUNT=$(echo $ACCESS_TOKEN | tr -cd '.' | wc -c)

if [ "$DOT_COUNT" = "2" ]; then
    print_result 0 "JWT token has valid structure (3 parts)"
else
    print_result 1 "JWT token has invalid structure"
fi
echo ""

# Test 7: Duplicate username registration
echo "Test 7: Duplicate Username Registration"
DUPLICATE_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"different@example.com\",
    \"displayName\": \"Different User\",
    \"username\": \"$TEST_USER\",
    \"password\": \"$TEST_PASSWORD\"
  }" --max-time $TIMEOUT)

if echo $DUPLICATE_RESPONSE | grep -q "already registered"; then
    print_result 0 "Duplicate username rejected correctly"
else
    print_result 1 "Duplicate username should be rejected"
fi
echo ""

# Test 8: Duplicate email registration
echo "Test 8: Duplicate Email Registration"
DUPLICATE_EMAIL=$(curl -s -X POST "$BACKEND_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"displayName\": \"Different User\",
    \"username\": \"differentuser\",
    \"password\": \"$TEST_PASSWORD\"
  }" --max-time $TIMEOUT)

if echo $DUPLICATE_EMAIL | grep -q "already registered"; then
    print_result 0 "Duplicate email rejected correctly"
else
    print_result 1 "Duplicate email should be rejected"
fi
echo ""

# Test 9: Invalid email format
echo "Test 9: Invalid Email Format"
INVALID_EMAIL=$(curl -s -X POST "$BACKEND_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"not-an-email\",
    \"displayName\": \"Test\",
    \"username\": \"testuser\",
    \"password\": \"$TEST_PASSWORD\"
  }" --max-time $TIMEOUT)

if echo $INVALID_EMAIL | grep -q "Invalid\|error\|required" -i; then
    print_result 0 "Invalid email format rejected"
else
    print_result 1 "Invalid email format should be rejected"
fi
echo ""

# Test 10: Short password
echo "Test 10: Short Password Validation"
SHORT_PASS=$(curl -s -X POST "$BACKEND_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test@example.com\",
    \"displayName\": \"Test\",
    \"username\": \"testuser\",
    \"password\": \"short\"
  }" --max-time $TIMEOUT)

if echo $SHORT_PASS | grep -q "Invalid\|error\|required" -i; then
    print_result 0 "Short password rejected"
else
    print_result 1 "Short password should be rejected"
fi
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
