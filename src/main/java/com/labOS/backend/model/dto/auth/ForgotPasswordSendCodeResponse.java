package com.labOS.backend.model.dto.auth;

import lombok.Data;

import java.io.Serializable;

/**
 * Forgot password send code response
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class ForgotPasswordSendCodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Generic success message (to prevent user enumeration)
     */
    private String message;

    public ForgotPasswordSendCodeResponse() {
        this.message = "If the account exists, we have sent a password reset email to your address.";
    }
}

