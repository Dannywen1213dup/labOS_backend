package com.labOS.backend.model.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * Forgot password reset response
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class ForgotPasswordResetResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Success message
     */
    private String message;

    public ForgotPasswordResetResponse() {
        this.message = "Password has been reset successfully. Please login with your new password.";
    }
}

