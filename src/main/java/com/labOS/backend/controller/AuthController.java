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
 * Handles user registration and login with email verification
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

    private static final String REDIS_VERIFY_CODE_PREFIX = "auth:verify:code:";
    private static final long VERIFY_CODE_EXPIRE_SECONDS = 300; // 5 minutes

    /**
     * Step 1: Check if email exists
     * Determines whether to proceed to login or registration flow
     *
     * @param request Check email request
     * @return Response indicating whether email exists
     */
    @PostMapping("/check-email")
    public BaseResponse<CheckEmailResponse> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        String email = request.getEmail();

        // Query database to check if email exists
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        boolean exists = user != null;
        CheckEmailResponse response = new CheckEmailResponse(exists);

        return ResultUtils.success(response);
    }

    /**
     * Branch A: Existing user login
     * Validates email and password, issues token upon success
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

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email or password is incorrect");
        }

        // Verify password using BCrypt
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        if (!encryptedPassword.equals(user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email or password is incorrect");
        }

        // Check if user is active
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Account is not active. Please verify your email.");
        }

        // Integrate Sa-Token: Execute login
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

        // Build response with actual Sa-Token
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
     * Branch B: New user registration initialization
     * Creates user with UNVERIFIED status, generates and sends verification code
     *
     * @param request Register init request
     * @return Response with email address
     */
    @PostMapping("/register/init")
    public BaseResponse<RegisterInitResponse> registerInit(@Valid @RequestBody RegisterInitRequest request) {
        log.info("=== registerInit method called ===");
        String email = request.getEmail();
        String password = request.getPassword();
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        Boolean legalAccepted = request.getLegalAccepted();

        // Validate legal terms acceptance
        if (!Boolean.TRUE.equals(legalAccepted)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "You must accept the legal terms to register");
        }

        // Check if email already exists
        User existingUser = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (existingUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Email already registered");
        }

        // Create new user with UNVERIFIED status
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLegalAccepted(legalAccepted ? 1 : 0);
        user.setStatus("UNVERIFIED");

        // Encrypt password using BCrypt
        String encryptedPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        user.setUserPassword(encryptedPassword);

        // Set default values
        user.setUserAccount(email); // Set email as userAccount for compatibility
        user.setUserRole("user");
        user.setUserName(firstName + " " + lastName);

        // Save user to database
        boolean saved = userService.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "Failed to create user");

        // Generate 6-digit verification code
        String verificationCode = generateVerificationCode();

        // Store verification code in Redis with 5-minute expiration
        String redisKey = REDIS_VERIFY_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(redisKey, verificationCode, VERIFY_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // Send verification code via email
        emailService.sendVerificationCode(email, verificationCode);
        log.info("Verification code generated for {}: {}", email, verificationCode); // Log for development/debugging

        RegisterInitResponse response = new RegisterInitResponse(email);
        return ResultUtils.success(response);
    }

    /**
     * Branch B: Verify email and finalize registration
     * Validates verification code, updates user status to ACTIVE, and issues token
     *
     * @param request     Register verify request
     * @param httpRequest HTTP servlet request
     * @return Token information and user profile
     */
    @PostMapping("/register/verify")
    public BaseResponse<AuthTokenResponse> registerVerify(@Valid @RequestBody RegisterVerifyRequest request,
                                                          HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String code = request.getCode();

        // Get verification code from Redis
        String redisKey = REDIS_VERIFY_CODE_PREFIX + email;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(storedCode)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Verification code has expired or does not exist");
        }

        // Verify code
        if (!code.equals(storedCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Verification code is incorrect");
        }

        // Delete verification code from Redis
        stringRedisTemplate.delete(redisKey);

        // Update user status to ACTIVE
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        user.setStatus("ACTIVE");
        boolean updated = userService.updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "Failed to activate user");

        // Integrate Sa-Token: Execute login
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

        // Create response with actual Sa-Token
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
     * Resend verification code
     * Generates and sends a new verification code to the user's email
     *
     * @param request Resend code request
     * @return Success message
     */
    @PostMapping("/register/resend-code")
    public BaseResponse<RegisterInitResponse> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        String email = request.getEmail();

        // Check if user exists and is unverified
        User user = userService.lambdaQuery()
                .eq(User::getEmail, email)
                .one();

        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }

        if (!"UNVERIFIED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "User is already verified");
        }

        // Generate new verification code
        String verificationCode = generateVerificationCode();

        // Store verification code in Redis
        String redisKey = REDIS_VERIFY_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(redisKey, verificationCode, VERIFY_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // Send verification code via email
        emailService.sendVerificationCode(email, verificationCode);
        log.info("Verification code resent for {}: {}", email, verificationCode); // Log for development/debugging

        RegisterInitResponse response = new RegisterInitResponse(email);
        return ResultUtils.success(response);
    }

    /**
     * User logout
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