# 完整测试指南 - Complete Testing Guide

## SaToken Authentication & S3 Upload Flow

This document provides a comprehensive guide for testing the entire authentication system, from user registration, login, password reset, to S3 file uploads. All API endpoints are documented with Postman test cases.

---

## 📋 Table of Contents

1. [System Flow Overview](#system-flow-overview)
2. [Environment Setup](#environment-setup)
3. [Testing Flows](#testing-flows)
4. [Detailed API Test Cases](#detailed-api-test-cases)
5. [Error Handling](#error-handling)
6. [Common Issues](#common-issues)

---

## System Flow Overview

### Complete Authentication Flows

```
A. Login Flow (Existing User)
   1. User submits email & password → /api/auth/login
   2. Backend validates credentials
   3. Returns SaToken upon success

B. Registration Flow (New User)
   1. Request verification code → /api/auth/send-code
   2. Receive code via email
   3. Submit registration with code → /api/auth/register
   4. Returns SaToken upon success

C. Forgot Password Flow
   1. Request reset code → /api/auth/forgot-password/send-code
   2. Receive reset token via email
   3. Submit new password with token → /api/auth/forgot-password/reset
   4. Login with new credentials

D. S3 Upload Flow (Requires SaToken)
   1. Generate presigned URL → /api/s3/folder/presigned-upload-url/*
   2. Upload file using presigned URL
```

### Key Security Features

- ✅ **Generic error messages** to prevent user enumeration attacks
- ✅ **Timing attack protection** with dummy hash computation
- ✅ **Email verification** required for registration
- ✅ **Token-based authentication** using SaToken
- ✅ **Automatic session invalidation** after password reset
- ✅ **Time-limited verification codes** (5 min for registration, 30 min for password reset)

---

## Environment Setup

### 1. Base Configuration

**Base URL**: `http://localhost:8101/api`

**Content-Type**: `application/json`

**Token Header**: `satoken` (SaToken value)

### 2. Postman Environment Variables

Set up the following environment variables in Postman:

| Variable Name | Initial Value | Description |
|---------------|---------------|-------------|
| `baseUrl` | `http://localhost:8101/api` | API base URL |
| `satoken` | (empty) | Will be extracted from login/register response |
| `userId` | (empty) | Will be extracted from login/register response |
| `userEmail` | `test@example.com` | Test email address |
| `verificationCode` | (empty) | Verification code from email/logs |
| `resetToken` | (empty) | Password reset token from email/logs |

### 3. Required System Configuration

Ensure the following are configured in `application.yml`:

```yaml
# Email Service (for sending verification codes)
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

# Redis (for storing verification codes)
spring:
  redis:
    host: localhost
    port: 6379
    database: 0

# AWS S3 (for file uploads)
aws:
  s3:
    accessKey: ${AWS_ACCESS_KEY:xxx}
    secretKey: ${AWS_SECRET_KEY:xxx}
    region: us-east-1
    bucket: labosfrontdemo1

# SaToken Configuration
sa-token:
  token-name: satoken
  timeout: 2592000  # 30 days
```

---

## Testing Flows

### Flow A: Login (Existing User)

#### Step A.1: Login

**Endpoint**: `POST /api/auth/login`

**Purpose**: Authenticate existing user with email and password

**Request Body**:
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isLogin": true,
    "loginId": "1751234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1751234567890,
      "userName": "John Doe",
      "userRole": "user",
      "createTime": "2024-01-01T10:00:00",
      "updateTime": "2024-01-01T10:00:00"
    }
  },
  "message": "ok"
}
```

**Error Response** (HTTP 200):
```json
{
  "code": 40000,
  "data": null,
  "message": "Email or password is incorrect"
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/login`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "{{userEmail}}",
  "password": "password123"
}
```

**Extract SaToken (Tests Tab)**:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0 && jsonData.data.tokenValue) {
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
        console.log("SaToken saved:", jsonData.data.tokenValue);
    }
}
```

**Security Notes**:
- ✅ Generic error message prevents user enumeration
- ✅ Timing attack protection with dummy hash computation
- ✅ Only ACTIVE users can login

---

### Flow B: Registration (New User)

#### Step B.1: Send Verification Code

**Endpoint**: `POST /api/auth/send-code`

**Purpose**: Send verification code to email for registration. Checks if email is already registered.

**Request Body**:
```json
{
  "email": "newuser@example.com"
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "email": "newuser@example.com",
    "message": "Verification code has been sent. Please check your email."
  },
  "message": "ok"
}
```

**Error Response - Email Already Exists** (HTTP 200):
```json
{
  "code": 40000,
  "data": null,
  "message": "Email is already registered. Please login or reset your password."
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/send-code`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "newuser@example.com"
}
```

**Important Notes**:
- Verification code is valid for **5 minutes**
- If email service is not configured, code will be printed in server logs
- Search logs for: `Verification code generated for registration: email=..., code=XXXXXX`

---

#### Step B.2: Register with Verification Code

**Endpoint**: `POST /api/auth/register`

**Purpose**: Create new user account with email verification, automatically login after success

**Request Body**:
```json
{
  "email": "newuser@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "code": "123456",
  "firstName": "John",
  "lastName": "Doe",
  "legalAccepted": true
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "tokenName": "satoken",
    "tokenValue": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isLogin": true,
    "loginId": "1751234567890",
    "tokenTimeout": 2592000,
    "userProfile": {
      "id": 1751234567890,
      "userName": "John Doe",
      "userRole": "user",
      "createTime": "2024-01-01T10:00:00",
      "updateTime": "2024-01-01T10:00:00"
    }
  },
  "message": "ok"
}
```

**Error Responses**:

1. **Passwords Don't Match**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Passwords do not match"
}
```

2. **Invalid Verification Code**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Verification code is incorrect"
}
```

3. **Expired Verification Code**:
```json
{
  "code": 50000,
  "data": null,
  "message": "Verification code has expired or does not exist. Please request a new code."
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/register`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "newuser@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "code": "{{verificationCode}}",
  "firstName": "John",
  "lastName": "Doe",
  "legalAccepted": true
}
```

**Extract SaToken (Tests Tab)**:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.code === 0 && jsonData.data.tokenValue) {
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
        console.log("SaToken saved:", jsonData.data.tokenValue);
    }
}
```

**Validation Rules**:
- ✅ Email must be valid format
- ✅ Password must be at least 8 characters
- ✅ Password and confirmPassword must match
- ✅ Verification code must be exactly 6 digits
- ✅ Legal terms must be accepted

---

### Flow C: Forgot Password

#### Step C.1: Send Password Reset Code

**Endpoint**: `POST /api/auth/forgot-password/send-code`

**Purpose**: Send password reset token to email. Returns generic message to prevent user enumeration.

**Request Body**:
```json
{
  "email": "test@example.com"
}
```

**Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "message": "If the account exists, we have sent a password reset email to your address."
  },
  "message": "ok"
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/forgot-password/send-code`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "{{userEmail}}"
}
```

**Important Notes**:
- ✅ Always returns success message (security measure)
- Reset token is valid for **30 minutes**
- If email service is not configured, token will be printed in server logs
- Search logs for: `Password reset token generated for: email=..., token=XXXXXX`

---

#### Step C.2: Reset Password with Token

**Endpoint**: `POST /api/auth/forgot-password/reset`

**Purpose**: Reset user password using verification token

**Request Body**:
```json
{
  "email": "test@example.com",
  "token": "123456",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "message": "Password has been reset successfully. Please login with your new password."
  },
  "message": "ok"
}
```

**Error Responses**:

1. **Passwords Don't Match**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Passwords do not match"
}
```

2. **Invalid Reset Token**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Reset token is incorrect"
}
```

3. **Expired Reset Token**:
```json
{
  "code": 50000,
  "data": null,
  "message": "Reset token has expired or does not exist. Please request a new reset code."
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/forgot-password/reset`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "email": "{{userEmail}}",
  "token": "{{resetToken}}",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

**Security Features**:
- ✅ All existing sessions are automatically logged out after password reset
- ✅ Reset token is deleted after use (prevents reuse)
- ✅ New password must meet complexity requirements

---

### Flow D: User Logout

#### Step D.1: Logout

**Endpoint**: `POST /api/auth/logout`

**Purpose**: Invalidate current user session

**Request Headers**:
```
satoken: {{satoken}}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/auth/logout`
- Headers: `satoken: {{satoken}}`

---

### Flow E: S3 File Upload (Requires Authentication)

#### Step E.1: Generate Dataset Upload URL

**Endpoint**: `POST /api/s3/folder/presigned-upload-url/dataset`

**Purpose**: Generate presigned URL for uploading dataset files

**Request Headers**:
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "fileName": "my-dataset.csv",
  "expirationTime": 3600000
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/datasets/1751234567890/my-dataset.csv?X-Amz-Algorithm=...",
  "message": "ok"
}
```

**File Storage Path**:
```
S3 Bucket: labosfrontdemo1
Path: labOS/datasets/{userId}/{sanitizedFileName}
Example: labOS/datasets/1751234567890/my-dataset.csv
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/s3/folder/presigned-upload-url/dataset`
- Headers:
  - `satoken: {{satoken}}`
  - `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "fileName": "my-dataset.csv",
  "expirationTime": 3600000
}
```

**Upload File Using Presigned URL**:
1. Copy the `data` field (presigned URL) from response
2. Create new Postman request (or use curl)
3. Method: `PUT`
4. URL: Paste presigned URL
5. Headers: `Content-Type: application/octet-stream`
6. Body: binary, select file to upload

**curl Example**:
```bash
curl -X PUT \
  "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/datasets/1751234567890/my-dataset.csv?X-Amz-Algorithm=..." \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/path/to/your/file.csv
```

---

#### Step E.2: Generate Benchmark Evaluation Upload URL

**Endpoint**: `POST /api/s3/folder/presigned-upload-url/benchmark-eval`

**Purpose**: Generate presigned URL for uploading benchmark evaluation files

**Request Headers**:
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "fileName": "evaluation-result.json",
  "expirationTime": 3600000
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/evaluation-result.json?X-Amz-Algorithm=...",
  "message": "ok"
}
```

**File Storage Path**:
```
S3 Bucket: labosfrontdemo1
Path: labOS/benchmark-eval/{userId}/{sanitizedFileName}
Example: labOS/benchmark-eval/1751234567890/evaluation-result.json
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval`
- Headers:
  - `satoken: {{satoken}}`
  - `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "fileName": "evaluation-result.json",
  "expirationTime": 3600000
}
```

---

#### Step E.3: Batch Generate Benchmark Evaluation URLs

**Endpoint**: `POST /api/s3/folder/presigned-upload-url/benchmark-eval/batch`

**Purpose**: Generate multiple presigned URLs in one request

**Request Headers**:
```
satoken: {{satoken}}
Content-Type: application/json
```

**Request Body**:
```json
{
  "fileNames": [
    "file1.json",
    "file2.json",
    "file3.json"
  ],
  "expirationTime": 3600000
}
```

**Success Response** (HTTP 200):
```json
{
  "code": 0,
  "data": {
    "entries": [
      {
        "fileName": "file1.json",
        "sanitizedFileName": "file1.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file1.json?X-Amz-Algorithm=..."
      },
      {
        "fileName": "file2.json",
        "sanitizedFileName": "file2.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file2.json?X-Amz-Algorithm=..."
      },
      {
        "fileName": "file3.json",
        "sanitizedFileName": "file3.json",
        "presignedUrl": "https://labosfrontdemo1.s3.us-east-1.amazonaws.com/labOS/benchmark-eval/1751234567890/file3.json?X-Amz-Algorithm=..."
      }
    ]
  },
  "message": "ok"
}
```

**Postman Configuration**:
- Method: `POST`
- URL: `{{baseUrl}}/s3/folder/presigned-upload-url/benchmark-eval/batch`
- Headers:
  - `satoken: {{satoken}}`
  - `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "fileNames": [
    "file1.json",
    "file2.json",
    "file3.json"
  ],
  "expirationTime": 3600000
}
```

---

## Detailed API Test Cases

### Complete Test Sequences

#### Scenario 1: New User Registration Flow

```
1. POST /api/auth/send-code
   → Send verification code to email
   → Check email or server logs for code
   
2. POST /api/auth/register
   → Register with email, password, code, and user info
   → Extract SaToken from response → save to {{satoken}}
   
3. POST /api/s3/folder/presigned-upload-url/dataset
   → Use {{satoken}} to generate upload URL
   
4. PUT <presigned-url>
   → Upload file using presigned URL
   
5. POST /api/auth/logout
   → Logout user
```

#### Scenario 2: Existing User Login and Upload

```
1. POST /api/auth/login
   → Login with email and password
   → Extract SaToken from response → save to {{satoken}}
   
2. POST /api/s3/folder/presigned-upload-url/benchmark-eval
   → Use {{satoken}} to generate upload URL
   
3. PUT <presigned-url>
   → Upload file using presigned URL
```

#### Scenario 3: Password Reset Flow

```
1. POST /api/auth/forgot-password/send-code
   → Request password reset
   → Check email or server logs for reset token
   
2. POST /api/auth/forgot-password/reset
   → Reset password with token and new password
   
3. POST /api/auth/login
   → Login with new password
   → Extract SaToken from response
```

---

## Error Handling

### Common Error Responses

#### 1. Unauthorized - Not Logged In

**HTTP Status**: `200` (SaToken returns 200 with error code)

**Response**:
```json
{
  "code": 40101,
  "data": null,
  "message": "Please login first: ..."
}
```

**Solution**: 
- Check if `satoken` header is present in request
- Check if token has expired (default 30 days)
- Re-login to get new token

---

#### 2. Invalid Credentials

**HTTP Status**: `200`

**Response**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Email or password is incorrect"
}
```

**Solution**: 
- Verify email and password are correct
- Note: Generic error message for security (prevents user enumeration)

---

#### 3. Verification Code Errors

**Invalid Code**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Verification code is incorrect"
}
```

**Expired Code**:
```json
{
  "code": 50000,
  "data": null,
  "message": "Verification code has expired or does not exist. Please request a new code."
}
```

**Solution**: 
- Registration code expires in 5 minutes
- Request new code via `/api/auth/send-code`

---

#### 4. Email Already Registered

**HTTP Status**: `200`

**Response**:
```json
{
  "code": 40000,
  "data": null,
  "message": "Email is already registered. Please login or reset your password."
}
```

**Solution**: 
- Use login flow instead
- Or use forgot password flow to reset password

---

## System Configuration Checklist

### ✅ Required Configuration

1. **Email Service Configuration** (`application.yml`)
   ```yaml
   spring:
     mail:
       host: smtp.gmail.com
       port: 587
       username: ${MAIL_USERNAME:}
       password: ${MAIL_PASSWORD:}
   ```
   - If not configured, verification codes will be printed in logs

2. **AWS S3 Configuration** (`application.yml`)
   ```yaml
   aws:
     s3:
       accessKey: ${AWS_ACCESS_KEY:xxx}
       secretKey: ${AWS_SECRET_KEY:xxx}
       region: us-east-1
       bucket: labosfrontdemo1
   ```

3. **Redis Configuration** (`application.yml`)
   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
       database: 0
   ```
   - Required for storing verification codes

4. **SaToken Configuration** (`application.yml`)
   ```yaml
   sa-token:
     token-name: satoken
     timeout: 2592000  # 30 days
   ```

---

## Postman Collection Setup

### Pre-request Script (Global)

Automatically add token to headers:
```javascript
// Get token from environment
var token = pm.environment.get("satoken");
if (token) {
    pm.request.headers.add({
        key: "satoken",
        value: token
    });
}
```

### Test Script (For Login/Register)

Automatically extract and save token:
```javascript
// Check response status
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Parse response and save token
var jsonData = pm.response.json();
if (jsonData.code === 0 && jsonData.data) {
    // Save token
    if (jsonData.data.tokenValue) {
        pm.environment.set("satoken", jsonData.data.tokenValue);
        pm.environment.set("userId", jsonData.data.loginId);
        console.log("SaToken saved:", jsonData.data.tokenValue);
    }
}
```

---

## Common Issues

### Q1: How to get verification code?

**A**: 
- **Method 1**: Check your email inbox
- **Method 2**: Check server logs (development environment)
- Search logs for: 
  - Registration: `Verification code generated for registration: email=..., code=XXXXXX`
  - Password Reset: `Password reset token generated for: email=..., token=XXXXXX`

### Q2: How long are codes valid?

**A**: 
- Registration verification code: **5 minutes**
- Password reset token: **30 minutes**
- If expired, request a new code

### Q3: What if email service is not configured?

**A**: 
- Codes will be printed in server logs
- Configure email service for production use

### Q4: How to pass SaToken in requests?

**A**: 
- Add header: `satoken: {tokenValue}`
- TokenValue is obtained from login or register response
- Save to Postman environment variable for convenience

### Q5: How to test file upload?

**A**: 
1. Call presigned URL generation endpoint to get URL
2. Use PUT method with the presigned URL
3. Set Body to binary and select file to upload

### Q6: Where are uploaded files stored?

**A**: 
- Dataset files: `labOS/datasets/{userId}/{fileName}`
- Benchmark eval files: `labOS/benchmark-eval/{userId}/{fileName}`
- Bucket: `labosfrontdemo1`
- Region: `us-east-1`

### Q7: Why do I get 401 Unauthorized?

**A**: 
- Check if `satoken` header is present
- Check if token has expired (default: 30 days)
- Re-login to get new token

### Q8: Why does login always say "incorrect" even with right password?

**A**: 
- This is a security feature (prevents user enumeration)
- Check email spelling carefully
- Ensure password is exactly as registered
- Check user status is ACTIVE

---

## Testing Checklist

### Registration Flow
- [ ] Can send verification code to new email
- [ ] Verification code is received (email or logs)
- [ ] Cannot register with already-registered email
- [ ] Cannot register with invalid code
- [ ] Cannot register with expired code
- [ ] Can successfully register with valid code
- [ ] Automatically logged in after registration
- [ ] SaToken is returned after registration

### Login Flow
- [ ] ACTIVE users can login successfully
- [ ] Wrong email/password shows generic error
- [ ] Non-existent email shows generic error
- [ ] SaToken is returned after login
- [ ] Token can be used for authenticated requests

### Password Reset Flow
- [ ] Can request password reset for existing email
- [ ] Reset token is received (email or logs)
- [ ] Generic message shown regardless of email existence
- [ ] Cannot reset with invalid token
- [ ] Cannot reset with expired token
- [ ] Can successfully reset password with valid token
- [ ] All sessions are logged out after reset
- [ ] Can login with new password

### S3 Upload Flow
- [ ] Can generate dataset upload URL with valid token
- [ ] Can generate benchmark eval upload URL with valid token
- [ ] Can batch generate multiple URLs
- [ ] Cannot generate URLs without token
- [ ] Presigned URL can successfully upload file
- [ ] Files are stored in correct user folder

### Security
- [ ] Unauthenticated requests to S3 endpoints are rejected
- [ ] Expired tokens are rejected
- [ ] Login errors don't reveal if user exists
- [ ] Password reset doesn't reveal if email exists
- [ ] Verification codes expire correctly
- [ ] Used verification codes cannot be reused

---

## Contact & Support

For issues, check:
- Server logs
- SaToken configuration documentation
- AWS S3 configuration documentation

---

**Last Updated**: 2024-12-06  
**Version**: 2.0.0

**Changes from v1.0.0**:
- Refactored registration flow (two-step: send code → register)
- Added forgot password flow
- Improved security with generic error messages
- Added timing attack protection
- Removed UNVERIFIED status (users are ACTIVE after email verification)
- Enhanced documentation with security best practices
