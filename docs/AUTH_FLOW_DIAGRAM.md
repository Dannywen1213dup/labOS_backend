# Authentication Flow Diagrams

This document provides visual diagrams of the authentication system flows using Mermaid.

---

## Complete Authentication Flow

```mermaid
graph TD
    subgraph A[A. Login Flow - Existing User]
        A1[User: Input Email & Password] --> A2{Frontend: Call /api/auth/login}
        A2 --> A3{Backend: Find User Record}
        A3 -- User Exists --> A4{Backend: Verify Password}
        A3 -- User Not Exists --> A5{Backend: Execute Dummy Hash}
        A4 -- Success --> A6[Backend: Issue SaToken]
        A4 -- Failed --> A7[Backend: Return Generic Error 401]
        A5 --> A7
        A6 --> A8[Frontend: Login Success]
        A7 --> A9[Frontend: Login Failed]
    end

    subgraph B[B. Registration Flow - New User]
        B1[User: Input Email] --> B2{Frontend: Call /api/auth/send-code}
        B2 --> B3{Backend: Check Email Exists}
        B3 -- Already Registered --> B4[Backend: Return Error/Guide to Login]
        B3 -- Not Registered --> B5{Backend: Generate & Store Code}
        B5 --> B6[Backend: Send Email]
        B6 --> B7[Backend: Return Success]
        
        B8[User: Input Email/Password/Code/Info] --> B9{Frontend: Call /api/auth/register}
        B9 --> B10{Backend: Verify Code}
        B10 -- Invalid --> B11[Backend: Return Error]
        B10 -- Valid --> B12{Backend: Hash Password & Create User}
        B12 --> A6
    end

    subgraph C[C. Forgot Password Flow]
        C1[User: Click Forgot Password/Input Email] --> C2{Frontend: Call /api/auth/forgot-password/send-code}
        C2 --> C3{Backend: Find User Record}
        C3 -- Not Exists --> C4[Backend: Return Generic Message]
        C3 -- Exists --> C5{Backend: Generate & Store Reset Token}
        C5 --> C6[Backend: Send Email with Token]
        C6 --> C4
        
        C7[User: Click Email Link/Input Token & New Password] --> C8{Frontend: Call /api/auth/forgot-password/reset}
        C8 --> C9{Backend: Verify Token}
        C9 -- Invalid --> C10[Backend: Return Error]
        C9 -- Valid --> C11{Backend: Hash New Password & Update User}
        C11 --> C12[Backend: Logout All Sessions]
        C12 --> C13[Backend: Return Reset Success]
    end
```

---

## Detailed Login Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend
    participant Database
    participant SaToken

    User->>Frontend: Enter email & password
    Frontend->>Backend: POST /api/auth/login
    Backend->>Database: Query user by email
    
    alt User Exists
        Database-->>Backend: Return user record
        Backend->>Backend: Hash input password
        Backend->>Backend: Compare with stored hash
        
        alt Password Correct
            Backend->>Database: Check user status (ACTIVE)
            alt Status is ACTIVE
                Backend->>SaToken: Execute login(userId)
                SaToken-->>Backend: Generate token
                Backend-->>Frontend: Return token & user profile
                Frontend-->>User: Login success
            else Status is not ACTIVE
                Backend-->>Frontend: Return error: Account not active
                Frontend-->>User: Show error
            end
        else Password Incorrect
            Backend-->>Frontend: Return generic error
            Frontend-->>User: Email or password incorrect
        end
    else User Not Exists
        Backend->>Backend: Execute dummy hash (prevent timing attack)
        Backend-->>Frontend: Return generic error
        Frontend-->>User: Email or password incorrect
    end
```

---

## Detailed Registration Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend
    participant Database
    participant Redis
    participant EmailService
    participant SaToken

    Note over User,SaToken: Step 1: Send Verification Code
    User->>Frontend: Enter email
    Frontend->>Backend: POST /api/auth/send-code
    Backend->>Database: Check if email exists
    
    alt Email Already Registered
        Database-->>Backend: Email exists
        Backend-->>Frontend: Return error
        Frontend-->>User: Email already registered
    else Email Not Registered
        Database-->>Backend: Email not exists
        Backend->>Backend: Generate 6-digit code
        Backend->>Redis: Store code (expire: 5 min)
        Backend->>EmailService: Send code to email
        Backend-->>Frontend: Return success
        Frontend-->>User: Check your email
        EmailService-->>User: Email with code
    end

    Note over User,SaToken: Step 2: Register with Code
    User->>Frontend: Enter email, password, code, info
    Frontend->>Backend: POST /api/auth/register
    Backend->>Backend: Validate passwords match
    Backend->>Redis: Get stored code by email
    
    alt Code Valid
        Redis-->>Backend: Return code
        Backend->>Backend: Verify code matches
        Backend->>Redis: Delete code (prevent reuse)
        Backend->>Database: Check email again
        Backend->>Backend: Hash password
        Backend->>Database: Create user (status: ACTIVE)
        Database-->>Backend: User created
        Backend->>SaToken: Execute login(userId)
        SaToken-->>Backend: Generate token
        Backend-->>Frontend: Return token & user profile
        Frontend-->>User: Registration success & auto-login
    else Code Invalid/Expired
        Backend-->>Frontend: Return error
        Frontend-->>User: Invalid or expired code
    end
```

---

## Detailed Forgot Password Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend
    participant Database
    participant Redis
    participant EmailService
    participant SaToken

    Note over User,SaToken: Step 1: Request Password Reset
    User->>Frontend: Enter email (forgot password)
    Frontend->>Backend: POST /api/auth/forgot-password/send-code
    Backend->>Database: Find user by email
    
    alt User Exists
        Database-->>Backend: Return user
        Backend->>Backend: Generate 6-digit token
        Backend->>Redis: Store token (expire: 30 min)
        Backend->>EmailService: Send token to email
        EmailService-->>User: Email with reset token
    else User Not Exists
        Database-->>Backend: User not found
        Backend->>Backend: Log warning (don't reveal)
    end
    
    Note over Backend: Always return generic success message
    Backend-->>Frontend: Return generic message
    Frontend-->>User: If account exists, email sent

    Note over User,SaToken: Step 2: Reset Password
    User->>Frontend: Enter email, token, new password
    Frontend->>Backend: POST /api/auth/forgot-password/reset
    Backend->>Backend: Validate passwords match
    Backend->>Redis: Get stored token by email
    
    alt Token Valid
        Redis-->>Backend: Return token
        Backend->>Backend: Verify token matches
        Backend->>Redis: Delete token (prevent reuse)
        Backend->>Database: Find user
        Backend->>Backend: Hash new password
        Backend->>Database: Update user password
        Backend->>SaToken: Logout all sessions (userId)
        Backend-->>Frontend: Return success
        Frontend-->>User: Password reset success
    else Token Invalid/Expired
        Backend-->>Frontend: Return error
        Frontend-->>User: Invalid or expired token
    end
```

---

## S3 Upload Flow (Authenticated)

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend
    participant SaToken
    participant AWS_S3

    User->>Frontend: Request to upload file
    Frontend->>Backend: POST /api/s3/folder/presigned-upload-url/dataset<br/>Headers: satoken
    Backend->>SaToken: Validate token
    
    alt Token Valid
        SaToken-->>Backend: Token valid, return userId
        Backend->>Backend: Sanitize filename
        Backend->>Backend: Build S3 path: labOS/datasets/{userId}/{filename}
        Backend->>AWS_S3: Generate presigned URL (expire: 1 hour)
        AWS_S3-->>Backend: Return presigned URL
        Backend-->>Frontend: Return presigned URL
        Frontend-->>User: Ready to upload
        
        User->>Frontend: Select file
        Frontend->>AWS_S3: PUT {presigned-url}<br/>Body: file binary
        AWS_S3-->>Frontend: Upload success
        Frontend-->>User: Upload complete
    else Token Invalid
        SaToken-->>Backend: Token invalid/expired
        Backend-->>Frontend: Return error: Please login first
        Frontend-->>User: Authentication required
    end
```

---

## Security Features Overview

```mermaid
graph LR
    A[Security Features] --> B[User Enumeration Prevention]
    A --> C[Timing Attack Protection]
    A --> D[Token-based Auth]
    A --> E[Code Expiration]
    A --> F[Session Management]
    
    B --> B1[Generic error messages]
    B --> B2[Same response time]
    
    C --> C1[Dummy hash computation]
    C --> C2[Consistent processing time]
    
    D --> D1[SaToken JWT]
    D --> D2[30-day expiration]
    D --> D3[Automatic refresh]
    
    E --> E1[Registration: 5 min]
    E --> E2[Password reset: 30 min]
    E --> E3[One-time use codes]
    
    F --> F1[Auto-logout on password reset]
    F --> F2[Manual logout endpoint]
    F --> F3[Token invalidation]
```

---

## API Endpoint Summary

| Endpoint | Method | Auth Required | Purpose |
|----------|--------|---------------|---------|
| `/api/auth/login` | POST | No | Login existing user |
| `/api/auth/send-code` | POST | No | Send registration verification code |
| `/api/auth/register` | POST | No | Register new user with code |
| `/api/auth/forgot-password/send-code` | POST | No | Send password reset token |
| `/api/auth/forgot-password/reset` | POST | No | Reset password with token |
| `/api/auth/logout` | POST | Yes | Logout current user |
| `/api/s3/folder/presigned-upload-url/dataset` | POST | Yes | Generate dataset upload URL |
| `/api/s3/folder/presigned-upload-url/benchmark-eval` | POST | Yes | Generate benchmark eval upload URL |
| `/api/s3/folder/presigned-upload-url/benchmark-eval/batch` | POST | Yes | Batch generate upload URLs |

---

## Database Schema

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar email UK "Primary login method"
        varchar userPassword "Hashed with MD5 + salt"
        varchar firstName
        varchar lastName
        tinyint legalAccepted "0=no, 1=yes"
        varchar status "ACTIVE, DISABLED"
        varchar userRole "user, admin, ban"
        datetime createTime
        datetime updateTime
    }
    
    POST {
        bigint id PK
        varchar title
        text content
        varchar tags
        int thumbNum
        int favourNum
        bigint userId FK
        datetime createTime
        datetime updateTime
    }
    
    POST_THUMB {
        bigint id PK
        bigint postId FK
        bigint userId FK
        datetime createTime
    }
    
    POST_FAVOUR {
        bigint id PK
        bigint postId FK
        bigint userId FK
        datetime createTime
    }
    
    USER ||--o{ POST : creates
    USER ||--o{ POST_THUMB : likes
    USER ||--o{ POST_FAVOUR : favours
    POST ||--o{ POST_THUMB : has
    POST ||--o{ POST_FAVOUR : has
```

---

## Redis Key Structure

| Key Pattern | Value | Expiration | Purpose |
|-------------|-------|------------|---------|
| `auth:register:code:{email}` | 6-digit code | 5 minutes | Registration verification |
| `auth:reset:token:{email}` | 6-digit token | 30 minutes | Password reset verification |

---

**Last Updated**: 2024-12-06  
**Version**: 2.0.0

