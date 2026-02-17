# Email Service Usage Guide

This guide explains how to configure and use the email service in the labOS backend system.

## Table of Contents

1. [Configuration](#configuration)
2. [Email Service Interface](#email-service-interface)
3. [Editing Email Content](#editing-email-content)
4. [Triggering Email Sending](#triggering-email-sending)
5. [Examples](#examples)

---

## Configuration

### 1. Configure SMTP Settings

Edit `src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com  # Gmail SMTP server
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}  # Your Gmail address
    password: ${MAIL_PASSWORD:your-app-password}     # Gmail App Password (not regular password)
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

### 2. Gmail Setup

For Gmail, you need to:

1. **Enable 2-Factor Authentication** on your Google account
2. **Generate an App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Enter "labOS Backend" as the name
   - Copy the generated 16-character password
   - Use this password in `application.yml` as `MAIL_PASSWORD`

### 3. Environment Variables (Recommended)

Instead of hardcoding credentials, use environment variables:

```bash
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

Or set them in your IDE run configuration or `.env` file.

---

## Email Service Interface

The email service is defined in:
- **Interface**: `src/main/java/com/labOS/backend/service/EmailService.java`
- **Implementation**: `src/main/java/com/labOS/backend/service/impl/EmailServiceImpl.java`

### Current Method

```java
void sendVerificationCode(String toEmail, String code);
```

---

## Editing Email Content

### Location

Edit the email content in:
```
src/main/java/com/labOS/backend/service/impl/EmailServiceImpl.java
```

### Current Implementation

The `sendVerificationCode` method sends a simple text email. To modify the content, edit the `message.setText()` section:

```java
@Override
@Async
public void sendVerificationCode(String toEmail, String code) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("labOS - Email Verification Code");
        
        // EDIT THIS SECTION TO CHANGE EMAIL CONTENT
        message.setText(String.format(
            "Hello,\n\n" +
            "Your verification code is: %s\n\n" +
            "This code will expire in 5 minutes.\n\n" +
            "If you did not request this code, please ignore this email.\n\n" +
            "Best regards,\n" +
            "labOS Team",
            code
        ));
        
        mailSender.send(message);
        log.info("Verification code email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send verification code email to: {}", toEmail, e);
    }
}
```

### Adding HTML Email Support

To send HTML emails, you can add a new method or enhance the existing one:

```java
@Override
@Async
public void sendVerificationCodeHtml(String toEmail, String code) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("labOS - Email Verification Code");
        
        // HTML email content
        String htmlContent = String.format(
            "<html>" +
            "<body style='font-family: Arial, sans-serif;'>" +
            "<h2>Email Verification</h2>" +
            "<p>Hello,</p>" +
            "<p>Your verification code is: <strong style='font-size: 20px; color: #007bff;'>%s</strong></p>" +
            "<p>This code will expire in 5 minutes.</p>" +
            "<p>If you did not request this code, please ignore this email.</p>" +
            "<p>Best regards,<br>labOS Team</p>" +
            "</body>" +
            "</html>",
            code
        );
        
        helper.setText(htmlContent, true); // true = isHtml
        mailSender.send(message);
        log.info("HTML verification code email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send HTML email to: {}", toEmail, e);
    }
}
```

**Note**: Add this import if using HTML emails:
```java
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.mail.internet.MimeMessage;
```

---

## Triggering Email Sending

### 1. Automatic Triggers

Email sending is automatically triggered in the following scenarios:

#### A. User Registration (`registerInit`)
**Location**: `src/main/java/com/labOS/backend/controller/AuthController.java`

```java
@PostMapping("/register/init")
public BaseResponse<RegisterInitResponse> registerInit(@Valid @RequestBody RegisterInitRequest request) {
    // ... user creation logic ...
    
    // Generate verification code
    String verificationCode = generateVerificationCode();
    
    // Store in Redis
    stringRedisTemplate.opsForValue().set(redisKey, verificationCode, VERIFY_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    // THIS LINE TRIGGERS EMAIL SENDING
    emailService.sendVerificationCode(email, verificationCode);
    
    return ResultUtils.success(response);
}
```

#### B. Resend Verification Code (`resendCode`)
**Location**: `src/main/java/com/labOS/backend/controller/AuthController.java`

```java
@PostMapping("/register/resend-code")
public BaseResponse<RegisterInitResponse> resendCode(@Valid @RequestBody ResendCodeRequest request) {
    // ... validation logic ...
    
    // Generate new verification code
    String verificationCode = generateVerificationCode();
    
    // Store in Redis
    stringRedisTemplate.opsForValue().set(redisKey, verificationCode, VERIFY_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
    
    // THIS LINE TRIGGERS EMAIL SENDING
    emailService.sendVerificationCode(email, verificationCode);
    
    return ResultUtils.success(response);
}
```

### 2. Manual Trigger in Code

To send an email from any service or controller:

```java
@Resource
private EmailService emailService;

public void someMethod() {
    // Send verification code email
    emailService.sendVerificationCode("user@example.com", "123456");
}
```

### 3. Adding New Email Types

To add new email types (e.g., password reset, welcome email):

#### Step 1: Add method to interface
`src/main/java/com/labOS/backend/service/EmailService.java`

```java
public interface EmailService {
    void sendVerificationCode(String toEmail, String code);
    
    // Add new methods
    void sendPasswordResetCode(String toEmail, String code);
    void sendWelcomeEmail(String toEmail, String userName);
}
```

#### Step 2: Implement in service
`src/main/java/com/labOS/backend/service/impl/EmailServiceImpl.java`

```java
@Override
@Async
public void sendPasswordResetCode(String toEmail, String code) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("labOS - Password Reset Code");
        message.setText(String.format(
            "Hello,\n\n" +
            "Your password reset code is: %s\n\n" +
            "This code will expire in 10 minutes.\n\n" +
            "If you did not request this code, please ignore this email.\n\n" +
            "Best regards,\n" +
            "labOS Team",
            code
        ));
        mailSender.send(message);
        log.info("Password reset email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send password reset email to: {}", toEmail, e);
    }
}

@Override
@Async
public void sendWelcomeEmail(String toEmail, String userName) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to labOS!");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Welcome to labOS! We're excited to have you on board.\n\n" +
            "Your account has been successfully created and verified.\n\n" +
            "Best regards,\n" +
            "labOS Team",
            userName
        ));
        mailSender.send(message);
        log.info("Welcome email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send welcome email to: {}", toEmail, e);
    }
}
```

#### Step 3: Use in controller/service

```java
// In your controller or service
@Resource
private EmailService emailService;

public void resetPassword(String email) {
    String resetCode = generateResetCode();
    emailService.sendPasswordResetCode(email, resetCode);
}
```

---

## Examples

### Example 1: Send Email After User Registration Verification

In `AuthController.java`, after successful verification:

```java
@PostMapping("/register/verify")
public BaseResponse<AuthTokenResponse> registerVerify(...) {
    // ... verification logic ...
    
    user.setStatus("ACTIVE");
    userService.updateById(user);
    
    // Send welcome email
    emailService.sendWelcomeEmail(user.getEmail(), user.getUserName());
    
    // ... rest of the logic ...
}
```

### Example 2: Custom Email Template

Create a template method:

```java
private void sendCustomEmail(String toEmail, String subject, String body) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send email to: {}", toEmail, e);
    }
}
```

---

## Troubleshooting

### Email Not Sending

1. **Check SMTP credentials**: Verify `MAIL_USERNAME` and `MAIL_PASSWORD` are correct
2. **Check Gmail App Password**: Make sure you're using an App Password, not your regular password
3. **Check logs**: Look for error messages in the application logs
4. **Test SMTP connection**: Verify Gmail SMTP settings are correct (smtp.gmail.com:587)

### Common Errors

- **"Authentication failed"**: Wrong password or not using App Password
- **"Connection timeout"**: Firewall or network issue
- **"535-5.7.8 Username and Password not accepted"**: Need to enable "Less secure app access" or use App Password

---

## Important Notes

1. **Async Execution**: Email sending is asynchronous (`@Async`), so it won't block the main thread
2. **Error Handling**: Email failures are logged but don't throw exceptions to avoid breaking the main flow
3. **Development Mode**: If email is not configured, the system logs the verification code instead of sending email
4. **Production**: Always configure proper SMTP settings in production environment

---

**Last Updated**: 2025-12-01

