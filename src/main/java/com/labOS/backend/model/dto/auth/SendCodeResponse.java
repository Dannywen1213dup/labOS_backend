package com.labOS.backend.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Send verification code response
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email address
     */
    private String email;

    /**
     * Success message
     */
    private String message;

    public SendCodeResponse(String email) {
        this.email = email;
        this.message = "Verification code has been sent. Please check your email.";
    }
}

