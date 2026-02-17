package com.labOS.backend.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.exception.ThrowUtils;
import com.labOS.backend.model.dto.auth.*;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.LoginUserVO;
import com.labOS.backend.satoken.LoginMode;
import com.labOS.backend.satoken.SaTokenUtil;
import com.labOS.backend.satoken.UserModeUtil;
import com.labOS.backend.service.EmailService;
import com.labOS.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.labOS.backend.service.impl.UserServiceImpl.SALT;

/**
 * Authentication Controller
 * Handles user login, registration, and password reset with email verification
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailService emailService;

    private static final String REDIS_REGISTER_CODE_PREFIX = "auth:register:code:";
    private static final String REDIS_RESET_TOKEN_PREFIX = "auth:reset:token:";
    private static final long REGISTER_CODE_EXPIRE_SECONDS = 300; // 5 minutes
    private static final long RESET_TOKEN_EXPIRE_SECONDS = 1800; // 30 minutes

    /**
     * A. Login Flow
     * Validates email and password, issues token upon success
     * Returns generic error message to prevent user enumeration
     *
     * @param request     Auth login request
     * @param httpRequest HTTP servlet request
     * @return Token information and user profile
     */
    @PostMapping("/login")
    public BaseResponse<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request,
                                                 HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String password = request.getPassword();

        if (StringUtils.isAnyBlank(email, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // Query user by email
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        // Security measure: Perform dummy hash computation to prevent timing attacks
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        
        if (user == null) {
            // User does not exist - perform dummy hash to prevent timing side-channel attacks
            log.warn("Login attempt for non-existent user: {}", email);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email or password is incorrect");
        }

        // Verify password
        if (!encryptedPassword.equals(user.getUserPassword())) {
            log.warn("Failed login attempt for user: {}", email);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email or password is incorrect");
        }

        // Check if user is active
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Account is not active. Please contact support.");
        }

        // Execute Sa-Token login
        StpUtil.login(user.getId());

        // Store user information in session
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);
        SaTokenUtil.setUser(loginUserVO);

        // Set login mode based on user role
        if ("admin".equals(user.getUserRole())) {
            UserModeUtil.setLoginMode(LoginMode.ADMIN);
        } else {
            UserModeUtil.setLoginMode(LoginMode.REGULAR);
        }

        // Get Sa-Token information
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // Build response with Sa-Token
        AuthTokenResponse response = new AuthTokenResponse();
        response.setTokenName(tokenInfo.getTokenName());
        response.setTokenValue(tokenInfo.getTokenValue());
        response.setIsLogin(tokenInfo.getIsLogin());
        response.setLoginId(String.valueOf(tokenInfo.getLoginId()));
        response.setTokenTimeout(tokenInfo.getTokenTimeout());
        response.setUserProfile(loginUserVO);

        log.info("User logged in successfully: email={}, userId={}", email, user.getId());
        return ResultUtils.success(response);
    }

    /**
     * B. Registration Flow - Step 1: Send Verification Code
     * Generates and sends verification code to email for registration
     * Checks if email is already registered
     *
     * @param request Send code request
     * @return Success response with email
     */
    @PostMapping("/send-code")
    public BaseResponse<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        String email = request.getEmail();

        // Check if email already exists
        User existingUser = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                "Email is already registered. Please login or reset your password.");
        }

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();

        // Store verification code in Redis with 5-minute expiration
        String redisKey = REDIS_REGISTER_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(redisKey, verificationCode, 
            REGISTER_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // Send verification code via email
        emailService.sendVerificationCode(email, verificationCode);
        log.info("Verification code generated for registration: email={}, code={}", email, verificationCode);

        SendCodeResponse response = new SendCodeResponse(email);
        return ResultUtils.success(response);
    }

    /**
     * B. Registration Flow - Step 2: Register User
     * Validates verification code, creates user account, and issues token
     *
     * @param request     Register request
     * @param httpRequest HTTP servlet request
     * @return Token information and user profile
     */
    @PostMapping("/register")
    public BaseResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        String code = request.getCode();
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        Boolean legalAccepted = request.getLegalAccepted();

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }

        // Validate legal terms acceptance
        if (!Boolean.TRUE.equals(legalAccepted)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "You must accept the legal terms to register");
        }

        // Get verification code from Redis
        String redisKey = REDIS_REGISTER_CODE_PREFIX + email;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(storedCode)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Verification code has expired or does not exist. Please request a new code.");
        }

        // Verify code
        if (!code.equals(storedCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Verification code is incorrect");
        }

        // Delete verification code from Redis (prevent reuse)
        stringRedisTemplate.delete(redisKey);

        // Check if email already exists (double check)
        User existingUser = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email is already registered");
        }

        // Create new user with ACTIVE status
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLegalAccepted(legalAccepted ? 1 : 0);
        user.setStatus("ACTIVE"); // User is immediately active after email verification

        // Hash password
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        user.setUserPassword(encryptedPassword);

        // Set default values
        user.setUserAccount(email); // Set email as userAccount for compatibility
        user.setUserRole("user");
        user.setUserName(firstName + " " + lastName);

        // Save user to database
        boolean saved = userService.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "Failed to create user");

        // Execute Sa-Token login
        StpUtil.login(user.getId());

        // Store user information in session
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);
        SaTokenUtil.setUser(loginUserVO);

        // Set login mode
        UserModeUtil.setLoginMode(LoginMode.REGULAR);

        // Get Sa-Token information
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // Build response with Sa-Token
        AuthTokenResponse response = new AuthTokenResponse();
        response.setTokenName(tokenInfo.getTokenName());
        response.setTokenValue(tokenInfo.getTokenValue());
        response.setIsLogin(tokenInfo.getIsLogin());
        response.setLoginId(String.valueOf(tokenInfo.getLoginId()));
        response.setTokenTimeout(tokenInfo.getTokenTimeout());
        response.setUserProfile(loginUserVO);

        log.info("User registered and logged in successfully: email={}, userId={}", email, user.getId());
        return ResultUtils.success(response);
    }

    /**
     * Get current user info using Sa-Token
     *
     * @return Current logged-in user profile
     */
    @GetMapping("/user-info")
    public BaseResponse<LoginUserVO> getUserInfo() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User is not logged in");
        }

        LoginUserVO loginUserVO = SaTokenUtil.getUser();
        if (loginUserVO == null) {
            Long loginId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(loginId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "User not found");
            loginUserVO = userService.getLoginUserVO(user);
            SaTokenUtil.setUser(loginUserVO);
        }

        return ResultUtils.success(loginUserVO);
    }

    /**
     * C. Forgot Password Flow - Step 1: Send Reset Code
     * Generates and sends password reset token to email
     * Returns generic message to prevent user enumeration
     *
     * @param request Forgot password send code request
     * @return Generic success response
     */
    @PostMapping("/forgot-password/send-code")
    public BaseResponse<ForgotPasswordSendCodeResponse> forgotPasswordSendCode(
            @Valid @RequestBody ForgotPasswordSendCodeRequest request) {
        String email = request.getEmail();

        // Check if user exists
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (user != null) {
            // User exists - generate and send reset token
            String resetToken = generateVerificationCode();

            // Store reset token in Redis with 30-minute expiration
            String redisKey = REDIS_RESET_TOKEN_PREFIX + email;
            stringRedisTemplate.opsForValue().set(redisKey, resetToken, 
                RESET_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

            // Send reset code via email
            emailService.sendPasswordResetCode(email, resetToken);
            log.info("Password reset token generated for: email={}, token={}", email, resetToken);
        } else {
            // User does not exist - log but don't reveal to client
            log.warn("Password reset requested for non-existent email: {}", email);
        }

        // Always return generic success message (security measure to prevent user enumeration)
        ForgotPasswordSendCodeResponse response = new ForgotPasswordSendCodeResponse();
        return ResultUtils.success(response);
    }

    /**
     * C. Forgot Password Flow - Step 2: Reset Password
     * Validates reset token and updates user password
     *
     * @param request Forgot password reset request
     * @return Success response
     */
    @PostMapping("/forgot-password/reset")
    public BaseResponse<ForgotPasswordResetResponse> forgotPasswordReset(
            @Valid @RequestBody ForgotPasswordResetRequest request) {
        String email = request.getEmail();
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }

        // Get reset token from Redis
        String redisKey = REDIS_RESET_TOKEN_PREFIX + email;
        String storedToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(storedToken)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                "Reset token has expired or does not exist. Please request a new reset code.");
        }

        // Verify token
        if (!token.equals(storedToken)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Reset token is incorrect");
        }

        // Delete reset token from Redis (prevent reuse)
        stringRedisTemplate.delete(redisKey);

        // Find user
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        // Hash new password
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        user.setUserPassword(encryptedPassword);

        // Update user password
        boolean updated = userService.updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "Failed to update password");

        // Force logout all sessions for this user (security measure)
        try {
            StpUtil.logout(user.getId());
            log.info("All sessions for user {} have been logged out after password reset", user.getId());
        } catch (Exception e) {
            log.warn("Failed to logout user sessions: {}", e.getMessage());
        }

        log.info("Password reset successfully for user: email={}, userId={}", email, user.getId());
        
        ForgotPasswordResetResponse response = new ForgotPasswordResetResponse();
        return ResultUtils.success(response);
    }

    /**
     * User Logout
     * Invalidates the current user session
     *
     * @param request HTTP servlet request
     * @return Success indicator
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        try {
            // Get user info before logout for logging
            Long userId = SaTokenUtil.getUserId();

            // Execute Sa-Token logout
            StpUtil.logout();

            log.info("User logged out successfully: userId={}", userId);
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Logout failed");
        }
    }

    /**
     * Generate a random 6-digit verification code
     *
     * @return 6-digit string
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Ensures 6 digits
        return String.valueOf(code);
    }
}
