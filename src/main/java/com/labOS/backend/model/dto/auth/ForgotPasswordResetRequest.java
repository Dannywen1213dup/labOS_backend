package com.labOS.backend.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Forgot password reset request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class ForgotPasswordResetRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email address
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Reset token (verification code)
     */
    @NotBlank(message = "Reset token cannot be blank")
    @Size(min = 6, max = 6, message = "Reset token must be 6 digits")
    private String token;

    /**
     * New password
     */
    @NotBlank(message = "New password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    /**
     * Confirm new password
     */
    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}

