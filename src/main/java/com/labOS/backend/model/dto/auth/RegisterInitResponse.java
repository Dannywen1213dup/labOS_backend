package com.labOS.backend.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

/**
 * Register initialization response
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class RegisterInitResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email address that verification code was sent to
     */
    private String email;

    public RegisterInitResponse(String email) {
        this.email = email;
    }
}

