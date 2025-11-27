# Auth System Implementation Checklist

## 📋 Overview
This document provides a checklist of all implemented features and items that need to be completed for the new authentication system based on the design document.

---

## ✅ Completed Items

### 1. Data Transfer Objects (DTOs)
- ✅ Created `CheckEmailRequest` - Request to check if email exists
- ✅ Created `CheckEmailResponse` - Response indicating email existence
- ✅ Created `AuthLoginRequest` - Login request with email and password
- ✅ Created `RegisterInitRequest` - Registration initialization with user details
- ✅ Created `RegisterInitResponse` - Response after registration init
- ✅ Created `RegisterVerifyRequest` - Email verification code submission
- ✅ Created `ResendCodeRequest` - Request to resend verification code
- ✅ Created `AuthTokenResponse` - Token response after successful auth

**Location**: `src/main/java/com/labOS/backend/model/dto/auth/`

### 2. Entity Model Updates
- ✅ Added `email` field to User entity (unique, primary login method)
- ✅ Added `firstName` field to User entity
- ✅ Added `lastName` field to User entity
- ✅ Added `legalAccepted` field to User entity (TINYINT: 0/1)
- ✅ Added `status` field to User entity (UNVERIFIED/ACTIVE/DISABLED)

**Location**: `src/main/java/com/labOS/backend/model/entity/User.java`

### 3. Controller Implementation
- ✅ Created `AuthController` with following endpoints:
  - `/api/auth/check-email` - Check if email exists
  - `/api/auth/login` - User login
  - `/api/auth/register/init` - Initialize registration
  - `/api/auth/register/verify` - Verify email with code
  - `/api/auth/register/resend-code` - Resend verification code
  - `/api/auth/logout` - User logout

**Location**: `src/main/java/com/labOS/backend/controller/AuthController.java`

### 4. UserController Refactoring
- ✅ Removed `/user/register` endpoint (moved to AuthController)
- ✅ Removed `/user/login` endpoint (moved to AuthController)
- ✅ Removed `/user/logout` endpoint (moved to AuthController)
- ✅ Kept other user management endpoints (admin operations, profile updates)

**Location**: `src/main/java/com/labOS/backend/controller/UserController.java`

### 5. Database Migration Script
- ✅ Created SQL migration script with:
  - ALTER TABLE statements for new fields
  - Unique index on email field
  - Index on status field
  - Data migration for existing users

**Location**: `sql/migration_add_auth_fields.sql`

---

## 🚧 TODO: Items to Complete

### 1. Email Verification Service Implementation ⚠️ **HIGH PRIORITY**

**File Location**: `src/main/java/com/labOS/backend/controller/AuthController.java`

**TODO Markers Added At**:
- Line ~157: `// TODO: Send verification code via email`
- Line ~238: `// TODO: Send verification code via email`

**Required Steps**:

#### A. Add Email Dependencies
Add Spring Mail dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### B. Configure Email Settings
Add to `src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com  # Or your SMTP server
    port: 587
    username: your-email@example.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

#### C. Create Email Service
Create `src/main/java/com/labOS/backend/service/EmailService.java`:

```java
public interface EmailService {
    /**
     * Send verification code email
     * 
     * @param toEmail Recipient email address
     * @param code 6-digit verification code
     */
    void sendVerificationCode(String toEmail, String code);
}
```

Create implementation `src/main/java/com/labOS/backend/service/impl/EmailServiceImpl.java`:

```java
@Service
public class EmailServiceImpl implements EmailService {
    
    @Resource
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Override
    @Async  // Send emails asynchronously
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Verification Code");
        message.setText("Your verification code is: " + code + "\n\nThis code will expire in 5 minutes.");
        
        mailSender.send(message);
    }
}
```

#### D. Replace TODO in AuthController
Replace the TODO comments with actual service calls:

```java
// In registerInit method:
emailService.sendVerificationCode(email, verificationCode);

// In resendCode method:
emailService.sendVerificationCode(email, verificationCode);
```

---

### 2. Sa-Token Integration ⚠️ **HIGH PRIORITY**

**File Location**: `src/main/java/com/labOS/backend/controller/AuthController.java`

**TODO Markers Added At**:
- Line ~105: `// TODO: Integrate Sa-Token here`
- Line ~201: `// TODO: Integrate Sa-Token here`
- Line ~253: `// TODO: Integrate Sa-Token logout`

**Required Steps**:

#### A. Add Sa-Token Dependencies
Add to `pom.xml`:

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId>
    <version>1.37.0</version>
</dependency>
<!-- Redis integration for Sa-Token -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-dao-redis-jackson</artifactId>
    <version>1.37.0</version>
</dependency>
```

#### B. Configure Sa-Token
Add to `src/main/resources/application.yml`:

```yaml
sa-token:
  token-name: satoken
  timeout: 2592000  # 30 days in seconds
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true
```

#### C. Replace Mock Token Logic
In `login` method (around line 105):

```java
// Replace mock code with:
StpUtil.login(user.getId());
SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

AuthTokenResponse response = new AuthTokenResponse();
response.setTokenName(tokenInfo.getTokenName());
response.setTokenValue(tokenInfo.getTokenValue());
response.setIsLogin(tokenInfo.getIsLogin());
response.setLoginId(tokenInfo.getLoginId().toString());
response.setTokenTimeout(tokenInfo.getTokenTimeout());
response.setUserProfile(userService.getLoginUserVO(user));
```

In `registerVerify` method (around line 201):

```java
// Replace mock code with:
StpUtil.login(user.getId());
SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

AuthTokenResponse response = new AuthTokenResponse();
response.setTokenName(tokenInfo.getTokenName());
response.setTokenValue(tokenInfo.getTokenValue());
response.setIsLogin(tokenInfo.getIsLogin());
response.setLoginId(tokenInfo.getLoginId().toString());
response.setTokenTimeout(tokenInfo.getTokenTimeout());
response.setUserProfile(userService.getLoginUserVO(user));
```

In `logout` method (around line 253):

```java
// Replace with:
StpUtil.logout();
return ResultUtils.success(true);
```

---

### 3. Redis Configuration Verification

**Check**: Ensure Redis is properly configured for both verification codes and Sa-Token

**File**: `src/main/resources/application.yml`

**Required Configuration**:

```yaml
spring:
  redis:
    host: localhost  # Or your Redis server
    port: 6379
    password:  # Set if required
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0
```

---

### 4. Database Migration Execution ⚠️ **REQUIRED BEFORE TESTING**

**Steps**:
1. Backup your database
2. Review the migration script: `sql/migration_add_auth_fields.sql`
3. Execute the migration:
   ```bash
   mysql -u your_username -p your_database < sql/migration_add_auth_fields.sql
   ```
4. Verify the changes:
   ```sql
   DESCRIBE user;
   SHOW INDEX FROM user;
   ```

---

### 5. Testing Checklist

Once email service and Sa-Token are integrated, test the following flows:

#### Test Case 1: New User Registration Flow
1. ✅ POST `/api/auth/check-email` with new email → Should return `exists: false`
2. ✅ POST `/api/auth/register/init` with user details → Should send verification email
3. ✅ Check email inbox for verification code
4. ✅ POST `/api/auth/register/verify` with email and code → Should return token
5. ✅ GET `/user/get/login` with token → Should return user profile

#### Test Case 2: Existing User Login Flow
1. ✅ POST `/api/auth/check-email` with registered email → Should return `exists: true`
2. ✅ POST `/api/auth/login` with email and password → Should return token
3. ✅ GET `/user/get/login` with token → Should return user profile

#### Test Case 3: Verification Code Expiration
1. ✅ POST `/api/auth/register/init` → Get verification code
2. ✅ Wait 6 minutes (code expires after 5 minutes)
3. ✅ POST `/api/auth/register/verify` → Should return error
4. ✅ POST `/api/auth/register/resend-code` → Should send new code
5. ✅ POST `/api/auth/register/verify` with new code → Should succeed

#### Test Case 4: Error Handling
1. ✅ Login with incorrect password → Should return error
2. ✅ Register with existing email → Should return error
3. ✅ Verify with wrong code → Should return error
4. ✅ Register without accepting legal terms → Should return error

---

### 6. Security Considerations ⚠️ **IMPORTANT**

#### A. Password Encryption
- ⚠️ Currently using MD5 with salt (implemented)
- 🔄 **RECOMMENDED**: Migrate to BCrypt for better security

**Migration Steps**:
1. Add BCrypt dependency (already in Spring Security)
2. Update password encryption in `AuthController`:
   ```java
   import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
   
   // Replace MD5 with BCrypt
   BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
   String encryptedPassword = encoder.encode(password);
   
   // For verification:
   boolean matched = encoder.matches(inputPassword, storedPassword);
   ```

#### B. Rate Limiting
- 🔄 **RECOMMENDED**: Add rate limiting for:
  - `/api/auth/check-email` (prevent email enumeration)
  - `/api/auth/login` (prevent brute force)
  - `/api/auth/register/resend-code` (prevent spam)

#### C. CORS Configuration
- ✅ Check `CorsConfig.java` to ensure frontend domain is allowed

---

### 7. Frontend Integration Notes

**API Endpoint Summary**:

| Endpoint | Method | Purpose | Request Body | Response |
|----------|--------|---------|--------------|----------|
| `/api/auth/check-email` | POST | Check email exists | `{email}` | `{exists: boolean}` |
| `/api/auth/login` | POST | User login | `{email, password}` | `{token, userProfile}` |
| `/api/auth/register/init` | POST | Start registration | `{email, password, firstName, lastName, legalAccepted}` | `{email}` |
| `/api/auth/register/verify` | POST | Verify email | `{email, code}` | `{token, userProfile}` |
| `/api/auth/register/resend-code` | POST | Resend code | `{email}` | `{email}` |
| `/api/auth/logout` | POST | Logout | - | `{success}` |

**Frontend Flow**:
1. User enters email → Call `check-email`
2. If exists → Show password field → Call `login`
3. If not exists → Show registration form → Call `register/init` → Show verification code input → Call `register/verify`

---

## 📝 Additional Recommendations

### 1. Logging and Monitoring
- Add structured logging for authentication events
- Monitor failed login attempts
- Track verification code success/failure rates

### 2. Documentation
- Update API documentation (Swagger/OpenAPI)
- Document error codes and messages
- Create user-facing documentation

### 3. Testing
- Write unit tests for AuthController methods
- Write integration tests for the complete auth flow
- Add end-to-end tests with frontend

---

## 🎯 Priority Order for Completion

1. **HIGHEST PRIORITY**: 
   - Execute database migration
   - Implement Email Service
   - Integrate Sa-Token

2. **HIGH PRIORITY**:
   - Comprehensive testing
   - Security hardening (BCrypt, rate limiting)

3. **MEDIUM PRIORITY**:
   - Documentation updates
   - Monitoring setup

4. **LOW PRIORITY**:
   - Performance optimization
   - Additional features (password reset, etc.)

---

## 📞 Support

For questions or issues during implementation:
- Review the design document
- Check TODO markers in code
- Refer to this checklist

---

**Last Updated**: 2025-11-23
**Author**: Yifan Wen
**Project**: labOS Backend Authentication System

