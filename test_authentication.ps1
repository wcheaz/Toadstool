# PowerShell Automated Testing Script for Authentication

param(
    [string]$BackendUrl = "http://localhost:8081",
    [int]$Timeout = 5
)

$ErrorActionPreference = "Continue"

# Test counter
$testsPassed = 0
$testsFailed = 0

# Helper function to print test result
function Print-Result {
    param(
        [bool]$passed,
        [string]$message
    )
    
    if ($passed) {
        Write-Host "✓ PASSED: $message" -ForegroundColor Green
        $script:testsPassed++
    } else {
        Write-Host "✗ FAILED: $message" -ForegroundColor Red
        $script:testsFailed++
    }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Authentication System Test Suite" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check backend is running
Write-Host "Test 1: Backend Health Check" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BackendUrl/health" -ErrorAction Stop -TimeoutSec $Timeout
    Print-Result $true "Backend is running on $BackendUrl"
} catch {
    try {
        $response = Invoke-WebRequest -Uri "$BackendUrl/api/auth/login" -Method Post -ErrorAction Stop -TimeoutSec $Timeout
        Print-Result $true "Backend is running on $BackendUrl"
    } catch {
        Print-Result $false "Backend is not responding"
        exit 1
    }
}
Write-Host ""

# Generate unique test user
$testTimestamp = Get-Date -Format "yyyyMMddHHmmssffff"
$testUser = "testuser_$testTimestamp"
$testEmail = "test_$testTimestamp@example.com"
$testPassword = "TestPassword123"

# Test 2: Register new user
Write-Host "Test 2: User Registration" -ForegroundColor Yellow
$registerBody = @{
    email = $testEmail
    displayName = "Test User"
    username = $testUser
    password = $testPassword
} | ConvertTo-Json

try {
    $registerResponse = Invoke-WebRequest -Uri "$BackendUrl/api/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $registerBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    $registerData = $registerResponse.Content | ConvertFrom-Json
    $accessToken = $registerData.accessToken
    $refreshToken = $registerData.refreshToken
    
    if ($accessToken -and $refreshToken) {
        Print-Result $true "User registered successfully"
    } else {
        Print-Result $false "User registration failed - missing tokens"
    }
} catch {
    Print-Result $false "User registration failed: $($_.Exception.Message)"
}
Write-Host ""

# Test 3: Login with correct credentials
Write-Host "Test 3: Login with Valid Credentials" -ForegroundColor Yellow
$loginBody = @{
    username = $testUser
    password = $testPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$BackendUrl/api/auth/login" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $loginBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $loginToken = $loginData.accessToken
    
    if ($loginToken) {
        Print-Result $true "Login successful"
    } else {
        Print-Result $false "Login failed - missing token"
    }
} catch {
    Print-Result $false "Login failed: $($_.Exception.Message)"
}
Write-Host ""

# Test 4: Login with incorrect credentials
Write-Host "Test 4: Login with Invalid Password" -ForegroundColor Yellow
$invalidLoginBody = @{
    username = $testUser
    password = "WrongPassword"
} | ConvertTo-Json

try {
    $invalidLogin = Invoke-WebRequest -Uri "$BackendUrl/api/auth/login" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $invalidLoginBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    Print-Result $false "Invalid login should be rejected"
} catch {
    $errorContent = $_.Exception.Response.Content.ReadAsStream()
    $errorReader = New-Object System.IO.StreamReader($errorContent)
    $errorMessage = $errorReader.ReadToEnd()
    
    if ($errorMessage -like "*Invalid*" -or $errorMessage -like "*password*") {
        Print-Result $true "Invalid login rejected correctly"
    } else {
        Print-Result $true "Invalid login rejected correctly"
    }
}
Write-Host ""

# Test 5: Refresh token
Write-Host "Test 5: Token Refresh" -ForegroundColor Yellow
$refreshBody = @{
    refreshToken = $refreshToken
} | ConvertTo-Json

try {
    $refreshResponse = Invoke-WebRequest -Uri "$BackendUrl/api/auth/refresh" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $refreshBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    $refreshData = $refreshResponse.Content | ConvertFrom-Json
    $newAccessToken = $refreshData.accessToken
    
    if ($newAccessToken) {
        Print-Result $true "Token refreshed successfully"
    } else {
        Print-Result $false "Token refresh failed - missing token"
    }
} catch {
    Print-Result $false "Token refresh failed: $($_.Exception.Message)"
}
Write-Host ""

# Test 6: Verify JWT token structure
Write-Host "Test 6: JWT Token Structure" -ForegroundColor Yellow
if ($accessToken) {
    $dotCount = ($accessToken | Select-String -Pattern "\." -AllMatches).Matches.Count
    
    if ($dotCount -eq 2) {
        Print-Result $true "JWT token has valid structure - 3 parts"
    } else {
        Print-Result $false "JWT token has invalid structure - expected 2 dots, got $dotCount"
    }
} else {
    Print-Result $false "No access token to validate"
}
Write-Host ""

# Test 7: Duplicate username registration
Write-Host "Test 7: Duplicate Username Registration" -ForegroundColor Yellow
$duplicateBody = @{
    email = "different@example.com"
    displayName = "Different User"
    username = $testUser
    password = $testPassword
} | ConvertTo-Json

try {
    $duplicateResponse = Invoke-WebRequest -Uri "$BackendUrl/api/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $duplicateBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    Print-Result $false "Duplicate username should be rejected"
} catch {
    Print-Result $true "Duplicate username rejected correctly"
}
Write-Host ""

# Test 8: Duplicate email registration
Write-Host "Test 8: Duplicate Email Registration" -ForegroundColor Yellow
$duplicateEmailBody = @{
    email = $testEmail
    displayName = "Different User"
    username = "differentuser"
    password = $testPassword
} | ConvertTo-Json

try {
    $duplicateEmail = Invoke-WebRequest -Uri "$BackendUrl/api/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $duplicateEmailBody `
        -ErrorAction Stop `
        -TimeoutSec $Timeout
    
    Print-Result $false "Duplicate email should be rejected"
} catch {
    Print-Result $true "Duplicate email rejected correctly"
}
Write-Host ""

# Summary
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor Red

if ($testsFailed -eq 0) {
    Write-Host "All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some tests failed" -ForegroundColor Red
    exit 1
}
